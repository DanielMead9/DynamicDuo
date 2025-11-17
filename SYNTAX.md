# Message Flow Language (MFL) — Syntax v0.1

A tiny, student friendly language for describing security message flows and checking simple secrecy claims.
Files use the .flow extension.

##  Quick start
// Declarations
principal Alice, Bob;
secretkey kAB;
message m;

// Flow
Alice -> Bob : Enc(kAB, m);

// Checks
assert secret(kAB);
assert secret(m);


Run the tool to get:

an SVG diagram of the flow

pass/fail results for the assert secret(…) checks

An eavesdropping adversary is always assumed (you don’t declare it).

## File structure

A .flow file is a sequence of declarations followed by steps:

Declarations (what exists):

principal Alice, Bob;

secretkey kAB;

publickey pkA;

privatekey skA;

message m1, m2;

cert certA(pkA); (optional: mark a public key as certified)

Steps (what happens):

Send: Alice -> Bob : Enc(kAB, m1, m2);

Assert secrecy: assert secret(kAB);

Comments start with // and run to end of line.
Each statement ends with ;.

## Declarations
principal Alice, Bob;
secretkey kAB, kAS;
publickey  pkA;
privatekey skA;
message    m1, m2;
cert certA(pkA);

## Steps
### 4.1 Send
<sender> -> <recipient> : <payload> ;


Examples:

Alice -> Bob : m1;                     // sends m1 in the clear
Alice -> Bob : Enc(kAB, m1 || m2);      // encryption with concatenated (combined) messages
Bob   -> Alice : Sig(skA, m1);         // digital signature
Bob   -> Alice : Mac(kAB, m2);         // MAC

### 4.2 Assert secrecy
assert secret(<name>) ;


Examples:

assert secret(kAB);
assert secret(m1);

## Payloads (simplified)

In a send, the payload can be:

a single name: m

a call: Enc(k, m) or Sig(sk, m)

multiple items ( double vertical lines = concatenation): Enc(k, m1 || m2 || m3 )

## Built-in primitives (v0.1)

Enc(key, items...) — symmetric encryption

Mac(key, items...) — message authentication code

Sig(sk, items...) — digital signature

Hash(items...) — cryptographic hash

## Adversary model (informal)

Adversary always sees all messages (but not information hidden behind encryption)

Plaintext items leak immediately.

Enc(k, …) hides contents unless k is known.

If k leaks later, all Enc(k, …) become readable.

MAC/Signature don’t reveal the key; their arguments are visible unless encrypted.

Hash outputs are public.

8) Examples
8.1 Safe
principal Alice, Bob;
secretkey k; message m;

Alice -> Bob : Enc(k, m);

assert secret(k);   // PASS
assert secret(m);   // PASS

8.2 Leak
principal Alice, Bob;
secretkey k; message m;

Alice -> Bob : Enc(k, m), k;   // k sent in clear

assert secret(k);   // FAIL
assert secret(m);   // FAIL

9) Keywords & symbols

Keywords: principal, secretkey, publickey, privatekey, message, cert, assert, secret

Symbols: -> : , ( ) ;

Identifiers: [A-Za-z_][A-Za-z0-9_]*, case-sensitive

Comments: //

10) Errors (examples)

line 7: expected ';' after declaration

line 12: 'kBA' not declared (did you mean 'kAB'?)

line 9: expected 'X -> Y : expr;'

11) Diagram rules

Principals → boxes

Adversary (eavesdropper) → dashed ellipse

Sends → solid arrows with payload labels

Adversary taps → dashed lines


(FORMAL) — Reference for Professor / Advanced Users
Payload expressions

Can be:

Identifier: m

Call: Enc(k, m1 || m2)

Tuple/concatenation: (m1 || m2 || m3)



## EBNF Grammar (v0.2)
program        := { decl | step } ;

decl           := "principal" ident_list ";"
               | "secretkey" ident_list ";"
               | "publickey" ident_list ";"
               | "privatekey" ident_list ";"
               | "message"   ident_list ";"
               | "cert" IDENT "(" IDENT ")" ";" ;

step           := send | assert_secret ;

send           := IDENT "->" IDENT ":" expr ";" ;
assert_secret  := "assert" "secret" "(" IDENT ")" ";" ;

expr           := concat_expr ;

concat_expr    := term { "||" term } ;

term           := IDENT
               | IDENT "(" [ expr { "," expr } ] ")"
               | "(" expr ")" ;

ident_list     := IDENT { "," IDENT } ;
