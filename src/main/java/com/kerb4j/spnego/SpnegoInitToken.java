package com.kerb4j.spnego;

import com.kerb4j.Kerb4JException;
import org.apache.kerby.asn1.Asn1FieldInfo;
import org.apache.kerby.asn1.EnumType;
import org.apache.kerby.asn1.ExplicitField;
import org.apache.kerby.asn1.parse.Asn1Container;
import org.apache.kerby.asn1.parse.Asn1ParseResult;
import org.apache.kerby.asn1.parse.Asn1Parser;
import org.apache.kerby.asn1.type.Asn1Flags;
import org.apache.kerby.asn1.type.Asn1ObjectIdentifier;
import org.apache.kerby.asn1.type.Asn1OctetString;
import org.apache.kerby.kerberos.kerb.type.KrbSequenceType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.kerb4j.spnego.SpnegoInitToken.AuthorizationDataEntryField.*;

public class SpnegoInitToken extends KrbSequenceType {

    /**
     * The AuthorizationDataEntry's fields
     */
    private static Asn1FieldInfo[] fieldInfos = new Asn1FieldInfo[]{
            new ExplicitField(MECH_TYPES, KrbObjectIds.class),
            new ExplicitField(REQ_FLAGS, Asn1Flags.class),
            new ExplicitField(MECH_TOKEN, Asn1OctetString.class),
            new ExplicitField(MECH_LIST_MIC, Asn1OctetString.class),
    };

    public SpnegoInitToken(byte[] token) throws Kerb4JException {

        super(fieldInfos);

        try {

            if (token.length <= 0)
                throw new Kerb4JException("spnego.token.empty", null, null);

            if ((byte) 0x60 != token[0]) {
                throw new Kerb4JException("spnego.token.invalid", new Object[]{token[0]}, null);
            }

            Asn1ParseResult asn1ParseResult = Asn1Parser.parse(ByteBuffer.wrap(token));

            Asn1ParseResult item1 = ((Asn1Container) asn1ParseResult).getChildren().get(0);
            Asn1ObjectIdentifier asn1ObjectIdentifier = new Asn1ObjectIdentifier();
            asn1ObjectIdentifier.decode(item1);

            if (!asn1ObjectIdentifier.getValue().equals(SpnegoConstants.SPNEGO_OID))
                throw new Kerb4JException("spnego.token.invalid", null, null);

            Asn1ParseResult item2 = ((Asn1Container) asn1ParseResult).getChildren().get(1);

            decode(((Asn1Container) item2).getChildren().get(0));

        } catch (IOException e) {
            throw new Kerb4JException("spnego.token.malformed", null, e);
        }
    }

    public List<String> getMechTypes() {
        List<String> mechTypes = new ArrayList<String>();
        for (Asn1ObjectIdentifier objId : getFieldAs(MECH_TYPES, KrbObjectIds.class).getElements()) {
            mechTypes.add(objId.getValue());
        }
        return mechTypes;
    }

    public String getMechanism() {
        List<String> mechTypes = getMechTypes();
        return null == mechTypes || mechTypes.isEmpty() ? null : mechTypes.get(0);
    }

    public int getReqFlags() {
        Asn1Flags reqFlags = getFieldAs(REQ_FLAGS, Asn1Flags.class);
        return null == reqFlags ? 0 : reqFlags.getFlags();
    }

    public byte[] getMechToken() {
        return getFieldAsOctets(MECH_TOKEN);
    }

    public byte[] getMechListMIC() {
        return getFieldAsOctets(MECH_LIST_MIC);
    }

    /**
     * The possible fields
     */
    protected enum AuthorizationDataEntryField implements EnumType {
        MECH_TYPES,
        REQ_FLAGS,
        MECH_TOKEN,
        MECH_LIST_MIC;

        /**
         * {@inheritDoc}
         */
        @Override
        public int getValue() {
            return ordinal();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return name();
        }
    }

}
