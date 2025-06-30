import stun
import qrcode 
import socket
from crypto.keygen import create_key

UDP_PORT = 5005

def is_available(port, host='0.0.0.0'):
    """
    checks if a port is available
    """
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        try:
            s.bind((host, port))
            return True
        except OSError:
            return False
        
def get_public_ip():
    """
    gets the public IP address of the machine/router using STUN
    """
    _, public_ip, public_port = stun.get_ip_info(
        stun_host = 'stun.l.google.com',
        stun_port = 19302
    )
    return public_ip, public_port

def create_qr(s, output_path = 'assets/qr_code.png'):
    """
    creates a QR code
    """
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4,
    )
    qr.add_data(s)
    qr.make(fit=True)

    img = qr.make_image(fill_color="black", back_color="white")
    img.save(output_path)

def generate_qr():
    """
    makes a QR code with public key| IP address | port number and saves it as PNG file
    """
    pbk, _ = create_key()
    hex_pbk = pbk.encode().hex()

    if is_available(UDP_PORT):
        print('Port is available')

    ip, port = get_public_ip()
    qr_data = f"{hex_pbk}|{ip}|{port}"

    create_qr(qr_data)

    return qr_data




