import socket 
from network.port_info import LOCAL_PORT, get_public_ip
from crypto.keygen import sign_challenge
NO_OF_CONNECTIONS = 5

def read(conn, buffer_size=1024, stop=b'\n'):
    """
    Reads data from the connection until the last byte(s) match STOP.
    """
    data = b''
    stop_len = len(stop)
    while True:
        chunk = conn.recv(buffer_size)
        if not chunk:
            break
        data += chunk
        # Check if the last bytes of data match the stop sequence
        if data[-stop_len:] == stop:
            break
    return data


def listen():
    """
    Starts server listening
    """

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    ip = get_public_ip()
    server.bind((f'{ip}', LOCAL_PORT))
    server.listen(NO_OF_CONNECTIONS)

    print(f"Listening on port {LOCAL_PORT}..."  )
    while True:
        conn, addr = server.accept()
        print(f"Connection established from {addr}")
        # conn.sendall(b"b'\xb2\x8f\xfd\xf7eY\x9b?L6\x9a\x7f\xd1\xe7\xd9\x16\x89\xb9\x19\xaf\xf3\xd6\xd3\x9b\xa7\xbe\xf9\x16\xe3\x97\xe9\xdd3.a\xbaK\xbf)\xdf\xd1\xd5\x18\xac!\xaaEj\xa1\x15\x9f*\x04w\xd5\x01\xd1 \xf2\xa5~\xc1i\x08'")
        if addr :
            data = read(conn,1024)
            print(f"Received data: {data}")
            parts = data.decode().rstrip('\n').split('|')
            second_part_bytes = bytes.fromhex(parts[1])

            signature = sign_challenge(second_part_bytes)
            # print("Signature type:", type(signature))
            # print("Signature length:", len(signature))

            # Ensure signature is bytes and exactly 64 bytes
            if not isinstance(signature, bytes):
                raise ValueError("Signature must be bytes")
            if len(signature) != 64:
                raise ValueError("Signature must be 64 bytes")

            conn.sendall(signature)

