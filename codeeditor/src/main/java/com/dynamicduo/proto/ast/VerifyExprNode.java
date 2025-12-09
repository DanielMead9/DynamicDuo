// VerifyExprNode: Verify(pk, m, sig)
// We can initially treat this as opaque, for analysis purposes
package com.dynamicduo.proto.ast;

import java.util.List;

public final class VerifyExprNode extends SyntaxNode {
    private final IdentifierNode pk;
    private final SyntaxNode message;
    private final SyntaxNode signature;

    public VerifyExprNode(IdentifierNode pk, SyntaxNode message, SyntaxNode signature) {
        this.pk = pk;
        this.message = message;
        this.signature = signature;
    }

    public IdentifierNode getPk()     { return pk; }
    public SyntaxNode     getMessage(){ return message; }
    public SyntaxNode     getSignature(){ return signature; }

    @Override
    public String label() {
        return "Verify(" + pk.getName() + ", ...)";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(pk, message, signature);
    }
}
