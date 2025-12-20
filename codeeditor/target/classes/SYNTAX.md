# Syntax Document 
## Dynamic Duo — Merrimack College CSC Capstone

This document defines the exact syntax accepted by the protocol tool.
It explains how to declare roles, keys, and messages, and how to use the
supported cryptographic primitives.

--------------------------------------------------
1. OVERVIEW
--------------------------------------------------

A protocol written for this tool is composed of three sections:

roles:
keys:
messages:

These sections must appear in this order.

--------------------------------------------------
2. ROLES
--------------------------------------------------

The roles section defines the principals participating in the protocol.

Syntax:
roles:
  Alice, Bob

Rules:
- Role names are case-sensitive after declaration
- Roles must be declared before they are used
- Exactly two roles are supported

--------------------------------------------------
3. KEYS
--------------------------------------------------

The keys section declares cryptographic keys and their initial owners.

--------------------------------------------------
3.1 Shared Keys (Symmetric)
--------------------------------------------------

Shared keys represent symmetric encryption.
The same key is used for both encryption and decryption by all listed owners.

Syntax:
shared K_AB: Alice, Bob

Rules:
- Shared keys are known only to the listed roles
- The adversary does not initially know shared keys

--------------------------------------------------
3.2 Public / Private Keys (Asymmetric)
--------------------------------------------------

Public/private keys represent asymmetric cryptography.

Syntax:
public key pk_A: Alice
private key sk_A: Alice

Rules:
- Public keys are known to everyone, including the adversary
- Private keys are known only to their owner
- Public and private keys must follow the naming convention:

    pkX / skX

  where X is a single letter (A–Z) identifying the owner

This convention allows the system to correctly match public keys with
their corresponding private keys.

--------------------------------------------------
3.3 Key Syntax Summary
--------------------------------------------------

shared K_NAME: Role1, Role2
public key pkX: Role
private key skX: Role

--------------------------------------------------
4. MESSAGES
--------------------------------------------------

The messages section defines the sequence of messages exchanged by the roles.

Messages are processed in order.

Syntax:
Sender -> Receiver : statement

A message body may be:
- an expression
- an assignment

Assignment example:
c = Enc(K_AB, M1)

Assigned variables may be referenced in later messages.

--------------------------------------------------
4.1 Concatenation
--------------------------------------------------

Concatenation combines two values into a single new value.

Syntax:
m1 || m2

Concatenation is treated as opaque.
Knowing (m1 || m2) does not imply knowledge of m1 or m2 individually.

--------------------------------------------------
5. ENCRYPTION
--------------------------------------------------

Encryption transforms plaintext data into ciphertext so that only authorized
parties can read it.

Syntax:
Enc(key, message)

--------------------------------------------------
5.1 Symmetric Encryption
--------------------------------------------------

Alice -> Bob: Enc(K_AB, M1)

Only parties who know K_AB can decrypt the message.

--------------------------------------------------
5.2 Asymmetric Encryption
--------------------------------------------------

Alice -> Bob: Enc(pkB, M1)

Alice encrypts the message using Bob’s public key.
Only Bob’s private key (skB) can decrypt the ciphertext.

Knowing the public key does NOT allow decryption.

--------------------------------------------------
5.3 Message Authentication Code (MAC)
--------------------------------------------------

A MAC provides integrity and authenticity.

Syntax:
Mac(key, message)

Example:
Mac(K_AB, c)

Only parties who know the shared key can generate or verify the MAC.

--------------------------------------------------
5.4 Digital Signatures
--------------------------------------------------

Digital signatures provide authenticity and non-repudiation.

Syntax:
Sign(sk, message)
Verify(pk, message, signature)

Examples:
sig = Sign(sk_Alice, M1)
ok  = Verify(pk_Alice, M1, sig)

Verification produces a boolean result.

--------------------------------------------------
5.5 Hash Function
--------------------------------------------------

A cryptographic hash function irreversibly transforms a message into a fixed-length value

Hashing scrambles the input so that it cannot be reversed, and even a tiny
change to the input produces a completely different output.

Syntax:
Hash(message)

Example:
Alice -> Bob: c = Enc(K_AB, Hash(M1))

Bob can access only the hashed value, not the original message. He does not recover M1

--------------------------------------------------
6. EXAMPLE PROTOCOLS
--------------------------------------------------

--------------------------------------------------
Example 1 — Symmetric Encryption
--------------------------------------------------

roles:
  Alice, Bob

keys:
  shared K_AB: Alice, Bob

messages:
  Alice -> Bob: c = Enc(K_AB, M1)

Result:
Alice sends M1 securely to Bob.
The adversary cannot learn the message.

If Alice later sends:
Alice -> Bob: K_AB

This is catastrophic.
The adversary can now decrypt all previously observed ciphertexts.

--------------------------------------------------
Example 2 — Asymmetric Encryption Failure
--------------------------------------------------

roles:
  Alice, Bob

keys:
  public key pk_A: Alice
  private key sk_A: Alice

  public key pk_B: Bob
  private key sk_B: Bob

messages:
  Alice -> Bob: c = Enc(pk_B, M1)
  Bob   -> Alice: sk_B

Result:
Bob leaks his private key.
The adversary can now decrypt ciphertexts encrypted under pk_B.
