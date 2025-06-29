import os
from crypto.keygen import create_key

def test_keys():    
    """
    Tests the key generation function by checking if it creates keys correctly or not
    """

    ROOT = os.getcwd()
    CRYPTO = os.path.join(ROOT, 'crypto')
    KEY = os.path.join(CRYPTO, 'keys')
    PV_PATH = os.path.join(KEY, 'private_key.key')
    PB_PATH = os.path.join(KEY, 'public_key.key')
    
    if os.path.isdir(KEY):
        os.remove(PV_PATH)
        os.remove(PB_PATH)
        os.rmdir(KEY)


    pvk1, pbk1 = create_key()
    pvk2, pbk2 = create_key()

    assert pvk1.encode() == pvk2.encode()
    assert pbk1.encode() == pbk2.encode()
    assert len(pvk1.encode()) == 32
    assert len(pbk1.encode()) == 32

    