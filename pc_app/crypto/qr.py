import qrcode
from crypto.keygen import create_key
from network.port_info import get_public_ip, is_available, LOCAL_PORT

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
    _ , pbk = create_key()
    hex_pbk = pbk.encode().hex()

    if is_available(LOCAL_PORT):
        print('Port is available')

    ip = get_public_ip()
    qr_data = f"{hex_pbk}|{ip}|{LOCAL_PORT}"
    
    create_qr(qr_data)

    return qr_data




