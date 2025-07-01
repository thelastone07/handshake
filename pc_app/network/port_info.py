import socket 

LOCAL_PORT = 5005

def is_available(port, host='0.0.0.0'):
    """
    checks if a port is available
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        try:
            s.bind((host, port))
            return True
        except OSError:
            return False
        
def get_public_ip():
    """
    gets the public (local) IP address of the machine/router using STUN
    """
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try :
        s.connect(('8.8.8.8',80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip

