# Message Flow Language (MFL v1.0)
## Dynamic Duo — Merrimack College CSC Capstone
### Message Flow Language (MFL) Specification

MFL is a small domain-specific language for describing cryptographic message-passing protocols, generating:

* An AST

* A sequence diagram

* Starter Java code

* Security analysis (knowledge flow + secrecy assertions)

* This document describes:

* All valid syntactic constructs

* The formal grammar

* Examples of complete protocols

1. Overall Structure

A protocol file consists of four optional sections (order recommended but not required):

roles:
  Alice, Bob

keys:
  shared K_AB for Alice, Bob
  public pk_Alice for Alice
  private sk_Alice for Alice

assert:
  secret M_1 for Alice, Bob
  secret K_AB

messages:
  Alice -> Bob: c = Enc(K_AB, M_1)
  Bob   -> Alice: t = Mac(K_AB, c)

2. Roles Section
roles:
  Alice, Bob, Server


## Defines the principals that may send or receive messages.

3. Keys Section

## Define keys and who possesses them.

3.1 Shared Symmetric Keys
shared K_AB for Alice, Bob

3.2 Public Key Pairs
public  pk_Alice  for Alice
private sk_Alice  for Alice

3.3 Syntax Summary
shared IDENT for identList
public IDENT for IDENT
private IDENT for IDENT

## 4. Assertions Section

## Assertions declare terms that must remain secret.

4.1 Basic secrecy
secret M_1


Means: adversary must not learn M_1.

4.2 Restricted secrecy
secret M_1 for Alice, Bob


Means: only Alice + Bob may know M_1.

5. Messages Section

Each message:

Sender -> Receiver : statement


Example:

Alice -> Bob: c = Enc(K_AB, M_1)

6. Expressions and Crypto Primitives
6.1 Identifiers
K_AB
M_1
nonceA
pk_B

6.2 Concatenation
m1 || m2

6.3 Encryption
Enc(key, message)

6.4 MAC Tag
Mac(key, message)

6.5 Digital Signatures
Sign(sk, message)
Verify(pk, message, sig)

6.6 Hash Function
H(message)

6.7 Assignment
c = Enc(K_AB, M_1)
t = Mac(K_AB, c)
sig = Sign(sk_Alice, M_1 || nonce)

7. Formal Grammar (EBNF)

((You may include this exact version in your repo.))

protocol       ::= rolesDecl keysDecl? assertDecl? messagesDecl

rolesDecl      ::= "roles" ":" identList

keysDecl       ::= "keys" ":" keyLine+
keyLine        ::= "shared" IDENT "for" identList
                 | "public" IDENT "for" IDENT
                 | "private" IDENT "for" IDENT

assertDecl     ::= "assert" ":" assertLine+
assertLine     ::= "secret" IDENT ("for" identList)?

messagesDecl   ::= "messages" ":" message+
message        ::= IDENT "->" IDENT ":" stmt

stmt           ::= IDENT "=" expr | expr

expr           ::= concatExpr

concatExpr     ::= cryptoExpr ( "||" cryptoExpr )*
cryptoExpr     ::= encExpr
                 | macExpr
                 | signExpr
                 | verifyExpr
                 | hashExpr
                 | IDENT

encExpr        ::= "Enc" "(" expr "," expr ")"
macExpr        ::= "Mac" "(" expr "," expr ")"
signExpr       ::= "Sign" "(" IDENT "," expr ")"
verifyExpr     ::= "Verify" "(" IDENT "," expr "," expr ")"
hashExpr       ::= "H" "(" expr ")"

identList      ::= IDENT ("," IDENT)*

8. Examples
Example A — Symmetric Key Encryption
roles: Alice, Bob

keys:
  shared K_AB for Alice, Bob

assert:
  secret M_1 for Alice, Bob

messages:
  Alice -> Bob: c = Enc(K_AB, M_1)
  Bob   -> Alice: ack = Mac(K_AB, c)

Example B — Public Key Challenge–Response
roles: Alice, Bob

keys:
  public  pk_Alice for Alice
  private sk_Alice for Alice
  public  pk_Bob   for Bob
  private sk_Bob   for Bob

assert:
  secret nonceA for Alice
  secret nonceB

messages:
  Alice -> Bob: nA = nonceA
  Bob   -> Alice: s = Sign(sk_Bob, nA)
  Alice -> Bob: ok = Verify(pk_Bob, nA, s)