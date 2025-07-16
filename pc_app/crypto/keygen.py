import os 
from nacl.signing import SigningKey, VerifyKey

ROOT = os.getcwd()
CRYPTO = os.path.join(ROOT, 'crypto')
KEY = os.path.join(CRYPTO, 'keys')
PVKEY = os.path.join(KEY, 'private_key.key')
PBKEY = os.path.join(KEY, 'public_key.key') 

def create_key():
    """
    Checks whether there is already a key file in current directory. if not, 
    creates a new key file with a random key
    """

    if os.path.isdir(KEY):
        with open(PVKEY, 'rb') as f:
            loaded_private_key = SigningKey(f.read())
        with open(PBKEY, 'rb') as f:
            loaded_public_key = VerifyKey(f.read())
        return loaded_private_key, loaded_public_key
    
    private_key = SigningKey.generate()
    public_key = private_key.verify_key

    private_key_bytes = private_key.encode()
    public_key_bytes = public_key.encode()

    os.makedirs(KEY, exist_ok=True)

    with open(PVKEY, 'wb') as f:
        f.write(private_key_bytes)

    with open(PBKEY, 'wb') as f:
        f.write(public_key_bytes)

    return private_key, public_key


def sign_challenge(challenge : bytes) -> bytes:
    """
    Signs the challenge with the private key..Returns the signature
    """
    private_key, public_key = create_key()
    # print("public key ",public_key.encode().hex())
    # print("private key ",private_key.encode().hex())


    signed_challenge = private_key.sign(challenge)
    # try :
    #     message = public_key.verify(signed_challenge)
    #     print(signed_challenge.signature.hex())
    #     print("Verifying signature with public key...", message)
    # except Exception as e:
    #     print("Signature verification failed:", e)

    return signed_challenge.signature