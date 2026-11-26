package com.dau.labmanagement.server.network.tcp;

import com.dau.labmanagement.server.model.dao.MachineDao;
import com.dau.labmanagement.server.model.dao.StudentSessionDao;
import com.dau.labmanagement.server.model.service.StudentIdentityService;
import com.dau.labmanagement.shared.protocol.MessageType;
import com.dau.labmanagement.shared.protocol.ProtocolFrame;
import com.dau.labmanagement.shared.protocol.ProtocolVersion;
import com.dau.labmanagement.shared.protocol.codec.TcpFrameCodec;
import com.dau.labmanagement.shared.protocol.message.HelloAckMessage;
import com.dau.labmanagement.shared.protocol.message.HelloMessage;
import com.dau.labmanagement.shared.protocol.message.LoginAckMessage;
import com.dau.labmanagement.shared.protocol.message.LoginMessage;
import com.dau.labmanagement.shared.protocol.message.LoginStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Handles a connected Client independently of whether a student has logged in. */
public final class ClientSessionHandler implements TcpConnectionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ClientSessionHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Jdbi jdbi;
    private final StudentIdentityService identityService;
    private final String serverId;

    public ClientSessionHandler(Jdbi jdbi, String serverId) {
        this.jdbi = jdbi;
        this.identityService = new StudentIdentityService(jdbi);
        this.serverId = serverId;
    }

    @Override
    public void handle(Socket socket) throws IOException {
        socket.setSoTimeout(5_000);
        ProtocolFrame helloFrame = TcpFrameCodec.read(socket.getInputStream());
        if (helloFrame == null
                || helloFrame.type() != MessageType.HELLO
                || helloFrame.version() != ProtocolVersion.CURRENT) {
            throw new IOException("Client must begin with a version 1 HELLO frame");
        }

        HelloMessage hello = TcpFrameCodec.payloadAs(helloFrame, HelloMessage.class);
        UUID clientId = validateHello(hello);
        long machineId = jdbi.withExtension(MachineDao.class, dao -> dao.upsert(
                clientId,
                hello.hostname(),
                socket.getInetAddress().getHostAddress(),
                null));

        TcpFrameCodec.write(socket.getOutputStream(), new ProtocolFrame(
                MessageType.HELLO_ACK,
                ProtocolVersion.CURRENT,
                OBJECT_MAPPER.valueToTree(
                        new HelloAckMessage(serverId, ProtocolVersion.CURRENT))));
        socket.setSoTimeout(0);

        Long activeStudentSessionId = null;
        StudentIdentityService.Challenge challenge = null;
        try {
            ProtocolFrame frame;
            while ((frame = TcpFrameCodec.read(socket.getInputStream())) != null) {
                if (frame.version() != ProtocolVersion.CURRENT) {
                    throw new IOException("Unsupported protocol version on connected session");
                }
                if (frame.type() == MessageType.LOGIN) {
                    LoginMessage login = TcpFrameCodec.payloadAs(frame, LoginMessage.class);
                    LoginOutcome outcome = handleLogin(
                            socket, login, challenge, machineId);
                    challenge = outcome.challenge();
                    if (outcome.studentSessionId() != null) {
                        if (activeStudentSessionId != null) {
                            closeStudentSession(activeStudentSessionId);
                        }
                        activeStudentSessionId = outcome.studentSessionId();
                    }
                } else {
                    LOG.debug("Received {} from connected client {}", frame.type(), clientId);
                }
            }
        } finally {
            if (challenge != null) {
                challenge.clear();
            }
            if (activeStudentSessionId != null) {
                closeStudentSession(activeStudentSessionId);
            }
        }
    }

    private LoginOutcome handleLogin(
            Socket socket, LoginMessage login,
            StudentIdentityService.Challenge previousChallenge, long machineId) throws IOException {
        String studentId = login.studentId() == null ? "" : login.studentId().trim();
        if (studentId.isBlank() || studentId.length() > 50) {
            clear(previousChallenge);
            writeLoginAck(socket, rejected("Enter a valid student ID and password."));
            return new LoginOutcome(null, null);
        }

        if (login.nonce() == null && login.proof() == null) {
            clear(previousChallenge);
            StudentIdentityService.Challenge challenge = identityService.beginLogin(studentId);
            writeLoginAck(socket, challenge.acknowledgment());
            return new LoginOutcome(challenge, null);
        }
        if (previousChallenge == null || login.nonce() == null || login.proof() == null) {
            clear(previousChallenge);
            writeLoginAck(socket, rejected("The login request is invalid. Please try again."));
            return new LoginOutcome(null, null);
        }

        StudentIdentityService.Challenge challenge = previousChallenge;
        try {
            var identity = identityService.completeLogin(
                    challenge, studentId, login.nonce(), login.proof(), machineId);
            if (identity.isEmpty()) {
                writeLoginAck(socket, rejected(
                        "Sai MSSV/mật khẩu hoặc tài khoản đã bị khóa."));
                return new LoginOutcome(null, null);
            }
            StudentIdentityService.StudentIdentity student = identity.get();
            LoginAckMessage acknowledgment = new LoginAckMessage(
                    LoginStatus.AUTHENTICATED,
                    null,
                    null,
                    0,
                    student.studentSessionId(),
                    student.studentId(),
                    student.fullName(),
                    null);
            try {
                writeLoginAck(socket, acknowledgment);
            } catch (IOException exception) {
                closeStudentSession(student.studentSessionId());
                throw exception;
            }
            return new LoginOutcome(null, student.studentSessionId());
        } finally {
            challenge.clear();
        }
    }

    private static UUID validateHello(HelloMessage hello) throws IOException {
        if (hello.clientId() == null || hello.hostname() == null || hello.hostname().isBlank()) {
            throw new IOException("HELLO requires clientId and hostname");
        }
        try {
            return UUID.fromString(hello.clientId());
        } catch (IllegalArgumentException exception) {
            throw new IOException("HELLO clientId must be a UUID", exception);
        }
    }

    private void writeLoginAck(Socket socket, LoginAckMessage acknowledgment) throws IOException {
        TcpFrameCodec.write(socket.getOutputStream(), new ProtocolFrame(
                MessageType.LOGIN_ACK,
                ProtocolVersion.CURRENT,
                OBJECT_MAPPER.valueToTree(acknowledgment)));
    }

    private static LoginAckMessage rejected(String message) {
        return new LoginAckMessage(
                LoginStatus.REJECTED, null, null, 0,
                null, null, null, message);
    }

    private static void clear(StudentIdentityService.Challenge challenge) {
        if (challenge != null) {
            challenge.clear();
        }
    }

    private void closeStudentSession(long studentSessionId) {
        try {
            jdbi.useExtension(StudentSessionDao.class,
                    dao -> dao.closeSession(studentSessionId, OffsetDateTime.now()));
        } catch (RuntimeException exception) {
            LOG.error("Could not close the student session after TCP disconnect", exception);
        }
    }

    private record LoginOutcome(
            StudentIdentityService.Challenge challenge, Long studentSessionId) {
    }
}
