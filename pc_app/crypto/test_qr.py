from crypto.qr import generate_qr
import cv2 
from pyzbar.pyzbar import decode

def test_generate_qr():
    '''
    tests the generate qr_function
    '''

    qr_data = generate_qr()

    img = cv2.imread('assets/qr_code.png')
    qr_code = decode(img)

    for qr in qr_code :
        data = qr.data.decode('utf-8')

    assert data == qr_data 




