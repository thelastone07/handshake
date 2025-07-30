from crypto.qr import generate_qr
from network.listen import listen
import threading

def main():
    generate_qr()
    
    listen()
    print('Server is running...')
 

if __name__ == "__main__":
    main()