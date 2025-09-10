package cn.rukkit.network.core;
// import net.rwhps.server.io.GameInputStream;

// import net.rwhps.server.io.GameOutputStream;
// import net.rwhps.server.struct.SerializerTypeAll;
// import net.rwhps.server.util.Time;
// import net.rwhps.server.util.algorithms.digest.DigestUtils;
// import net.rwhps.server.util.math.RandomUtils;
// import net.rwhps.server.util.str.StringFilteringUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

import cn.rukkit.network.io.GameInputStream;
import cn.rukkit.network.io.GameOutputStream;
import cn.rukkit.util.math.RandomUtils;
import cn.rukkit.util.Time;

/**
 * Proof-of-Work Verify if it is a client (Only the most versatile part)
 *
 * I don't want this part to be abused
 * Cherish what you have at the moment
 *
 * @author Dr (dr@der.kim)
 */

public class NetConnectProofOfWork {
    // Uniqueness
    private int resultInt = Time.concurrentSecond();
    private final byte authenticateType;

    private final int initInt_1;
    private final int initInt_2;

    private final String outcome;
    private final String fixedInitial;
    private final int off;
    private final int maximumNumberOfCalculations;

    public class SerializerTypeAll {
        /**
         * Generic serializer interface
         * 
         * @param <T> Type to be serialized/deserialized
         */
        public interface TypeSerializer<T> {
            /**
             * Serialize and write data
             * 
             * @param paramDataOutput Output stream
             * @param objectParam     Data to be serialized
             * @throws IOException If an I/O error occurs
             */
            void write(GameOutputStream paramDataOutput, T objectParam) throws IOException;

            /**
             * Deserialize and read data
             * 
             * @param paramDataInput Input stream
             * @return Deserialized data
             * @throws IOException If an I/O error occurs
             */
            T read(GameInputStream paramDataInput) throws IOException;
        }
    }

    private static class ConnectKey {
        // 1.14
        @SuppressWarnings("unused")
        public static String connectKey_114(int paramInt) {
            return "c:" + paramInt +
                    "m:" + (paramInt * 87 + 24) +
                    "0:" + (paramInt * 44000) +
                    "1:" + paramInt +
                    "2:" + (paramInt * 13000) +
                    "3:" + (paramInt + 28000) +
                    "4:" + (paramInt * 75000) +
                    "5:" + (paramInt + 160000) +
                    "6:" + (paramInt * 850000) +
                    "t1:" + (paramInt * 44000) +
                    "d:" + (paramInt * 5);
        }

        // 1.15
        @SuppressWarnings("unused")
        public static String connectKeyNew_115_Test(int paramInt) {
            return "c:" + paramInt +
                    "m:" + (paramInt * 87 + 24) +
                    "0:" + (paramInt * 44000) +
                    "1:" + paramInt +
                    "2:" + (paramInt * 13000) +
                    "3:" + (paramInt + 28000) +
                    "4:" + (paramInt * 75000) +
                    "5:" + (paramInt + 160000) +
                    "6:" + (paramInt * 850000) +
                    "t1:" + (paramInt * 4000.0 * 11.0) +
                    "d:" + (paramInt * 5);
        }

        // 1.15.P10
        public static String connectKeyLast(int paramInt) {
            return "c:" + paramInt +
                    "m:" + (paramInt * 87 + 24) +
                    "0:" + (paramInt * 44000) +
                    "1:" + paramInt +
                    "2:" + (paramInt * 13000) +
                    "3:" + (paramInt + 28000) +
                    "4:" + (paramInt * 75000) +
                    "5:" + (paramInt + 160000) +
                    "6:" + (paramInt * 850000) +
                    "7:" + (paramInt * 1800000) +
                    "8:" + (paramInt * 3800000) +
                    "t1:" + (paramInt * 4000.0 * 11.0) +
                    "d:" + (paramInt * 5);
        }
    }

    public static final SerializerTypeAll.TypeSerializer<NetConnectProofOfWork> serializer = new SerializerTypeAll.TypeSerializer<NetConnectProofOfWork>() {
        @Override
        public void write(GameOutputStream paramDataOutput, NetConnectProofOfWork objectParam) throws IOException {
            paramDataOutput.writeByte(objectParam.authenticateType);
            switch (objectParam.authenticateType) {
                case 0:
                    paramDataOutput.writeInt(objectParam.initInt_1);
                    break;
                case 1:
                    paramDataOutput.writeInt(objectParam.initInt_2);
                    break;
                case 3:
                case 4:
                    paramDataOutput.writeInt(objectParam.initInt_1);
                    paramDataOutput.writeInt(objectParam.initInt_2);
                    paramDataOutput.writeString(objectParam.outcome);
                    break;
                case 5:
                case 6:
                    paramDataOutput.writeString(objectParam.fixedInitial);
                    paramDataOutput.writeInt(objectParam.off);
                    paramDataOutput.writeInt(objectParam.maximumNumberOfCalculations);
                    paramDataOutput.writeString(objectParam.outcome);
                    break;
                default:
                    break;
            }
        }

        @Override
        public NetConnectProofOfWork read(GameInputStream paramDataInput) throws IOException {
            byte authenticateType = paramDataInput.readByte();
            switch (authenticateType) {
                case 0:
                    return new NetConnectProofOfWork(authenticateType, paramDataInput.readInt(), 0);
                case 1:
                    return new NetConnectProofOfWork(authenticateType, 0, paramDataInput.readInt());
                case 3:
                case 4:
                    return new NetConnectProofOfWork(
                            authenticateType,
                            paramDataInput.readInt(),
                            paramDataInput.readInt(),
                            paramDataInput.readString());
                case 5:
                case 6:
                    return new NetConnectProofOfWork(
                            authenticateType,
                            paramDataInput.readString(),
                            paramDataInput.readInt(),
                            paramDataInput.readInt(),
                            paramDataInput.readString());
                default:
                    return new NetConnectProofOfWork();
            }
        }
    };

    public static byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available, but handle the exception just in case
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static String cutting(String input, int length) {
        if (input.length() <= length) {
            return input;
        }
        return input.substring(0, length);
    }

    public NetConnectProofOfWork() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // No use 4/6
        int authType = rand.nextInt(0, 6);
        if (authType == 2) {
            authType = 5;
        }

        this.authenticateType = (byte) authType;

        if (authenticateType == 0 || (authenticateType >= 2 && authenticateType <= 4) || authenticateType == 6) {
            initInt_1 = rand.nextInt();
        } else {
            initInt_1 = 0;
        }

        if (authenticateType == 1 || (authenticateType >= 2 && authenticateType <= 4)) {
            initInt_2 = rand.nextInt();
        } else {
            initInt_2 = 0;
        }

        if (authenticateType >= 3 && authenticateType <= 4) {
            outcome = cutting(new BigInteger(1, sha256(initInt_1 + "|" + initInt_2)).toString(16).toUpperCase(), 14);
            fixedInitial = "";
            off = 0;
            maximumNumberOfCalculations = 0;
        } else if (authenticateType >= 5 && authenticateType <= 6) {
            if (authenticateType == 6) {
                fixedInitial = RandomUtils.getRandomIetterString(4) + initInt_1;
            } else {
                fixedInitial = RandomUtils.getRandomIetterString(4);
            }
            off = rand.nextInt(0, 10);
            maximumNumberOfCalculations = rand.nextInt(0, 10000000);
            outcome = cutting(new BigInteger(1, sha256(fixedInitial + "" + off)).toString(16).toUpperCase(), 14);
        } else {
            outcome = "";
            fixedInitial = "";
            off = 0;
            maximumNumberOfCalculations = 0;
        }
    }

    // Type 0/1
    private NetConnectProofOfWork(byte authenticateType, int initInt_1, int initInt_2) {
        this.authenticateType = authenticateType;
        this.initInt_1 = initInt_1;
        this.initInt_2 = initInt_2;

        this.outcome = "";
        this.fixedInitial = "";
        this.off = 0;
        this.maximumNumberOfCalculations = 0;
    }

    // Type 3/4
    private NetConnectProofOfWork(byte authenticateType, int initInt_1, int initInt_2, String outcome) {
        this.authenticateType = authenticateType;
        this.initInt_1 = initInt_1;
        this.initInt_2 = initInt_2;
        this.outcome = outcome;

        this.fixedInitial = "";
        this.off = 0;
        this.maximumNumberOfCalculations = 0;
    }

    // Type 5/6
    private NetConnectProofOfWork(byte authenticateType, String fixedInitial, int off, int maximumNumberOfCalculations,
            String outcome) {
        this.authenticateType = authenticateType;
        this.fixedInitial = fixedInitial;
        this.off = off;
        this.maximumNumberOfCalculations = maximumNumberOfCalculations;
        this.outcome = outcome;

        this.initInt_1 = 0;
        this.initInt_2 = 0;
    }

    public boolean verifyPOWResult(int resultInt, int authenticateType, String offIn) {
        if (this.resultInt != resultInt || this.authenticateType != authenticateType) {
            return false;
        }

        switch (authenticateType) {
            case 0:
                return Integer.toString(initInt_1).equals(offIn);
            case 1:
                return Integer.toString(initInt_2).equals(offIn);
            case 2:
                return ConnectKey.connectKeyLast(initInt_1).equals(offIn);
            case 3:
            case 4:
                return outcome.equals(offIn);
            case 5:
            case 6:
                return this.off == Integer.parseInt(offIn);
            default:
                return false;
        }
    }

    public int getResultInt() {
        return resultInt;
    }

    public byte getAuthenticateType() {
        return authenticateType;
    }

    public int getInitInt_1() {
        return initInt_1;
    }

    public int getInitInt_2() {
        return initInt_2;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getFixedInitial() {
        return fixedInitial;
    }

    public int getOff() {
        return off;
    }

    public int getMaximumNumberOfCalculations() {
        return maximumNumberOfCalculations;
    }
}