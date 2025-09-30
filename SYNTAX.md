// Declarations , the first step of user input

principal Alice, Bob;
secretkey kAB;                 // shared secret
publickey  pkA; privatekey skA; // public/priv pair (pairing by name stem is optional)
message   m1, m2;


// Optional annotations

cert certA(pkA);               // certificate for pkA


// Protocol steps , second step of user input

Alice -> Bob : Enc(kAB, m1, m2);          // commas = concatenation
Bob   -> Alice: Sig(skA, m1);             // digital signature
Alice -> Bob : Mac(kAB, m2);              // MAC

// Stretch: invariants/checks

assert secret(kAB);
assert secret(m1);

// Built-ins (primitives)

* Enc(key, item1, item2, ...)

* Dec(key, ciphertext)

* Mac(key, item1, ...)

* VrfyMac(key, mac, item1, ...)

* Sig(sk, item1, ...)

* VrfySig(pk, sig, item1, ...)

* Hash(item1, ...)

* Nonce(name?) (optional convenience)

