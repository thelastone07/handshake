import socket
import qrcode 
from crypto.keygen import create_key


def create_qr():
    """
    creates a QR code with public key| IP address | port number and saves it as PNG file
    """
    pbk, _ = create_key()
    hex_pbk = pbk.encode().hex()