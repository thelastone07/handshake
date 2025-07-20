import socket 
from network.port_info import LOCAL_PORT, get_public_ip
from crypto.keygen import sign_challenge
import pyperclip
import select


NO_OF_CONNECTIONS = 5

TEXT_DATA = b'x\01'
IMAGE_DATA = b'x\02'
B_IMAGE_DATA = b'x\03'


def read(conn, stop=b'\x00', buffer_size = 1024):
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

def encode_data(data: bytes) -> bytes:
  encoded = bytearray()
  j = 0
  while j < len(data):
    code_idx = len(encoded)
    code_len = 1
    encoded.append(0)
    while j < len(data) and data[j] != 0 and code_len < 255:
      encoded.append(data[j])
      j = j + 1
      code_len = code_len + 1
    encoded[code_idx] = code_len
    if j < len(data) and data[j] == 0:
      j = j + 1
  encoded.append(0)
  return bytes(encoded)



def decode_data(data:bytes) ->bytes:
  data = data[:-1]
  decoded = bytearray()
  j = 0
  while j < len(data):
    code_len = data[j]
  
    j = j + 1
    decoded += data[j:j+code_len-1]

    j = j + code_len - 1

    if code_len < 255 and j < len(data): 
      decoded.append(0)
    
  return bytes(decoded)

def handle_image(data : bytes):
   '''
   helper function to handle image data
   '''




def listen():
    """
    Starts server listening
    """

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    ip = get_public_ip()
    server.bind((f'{ip}', LOCAL_PORT))
    server.listen(NO_OF_CONNECTIONS)
    server.setblocking(0)

    while True:
        conn, _ = server.accept()
        with conn:
            data = read(conn)
            
            parts = data.decode().rstrip('\n').split('|')
            second_part_bytes = bytes.fromhex(parts[1])

            signature = sign_challenge(second_part_bytes)

            # Ensure signature is bytes and exactly 64 bytes
            if not isinstance(signature, bytes):
                raise ValueError("Signature must be bytes")
            if len(signature) != 64:
                raise ValueError("Signature must be 64 bytes")

            conn.sendall(signature)
            '''
            currently pyperclip works with only text
            if you copy any image or such - it gives you an empty string
            '''
            last_clipboard_txt = ""
            last_clipboard_txt_recieved = ""
            clipboard_state = True
            while True :
                ''' 
                now the socket stays open 
                track whether you have to send or receive data
                in case you receive data - update the clipboard
                if the data is changed in clipboard - send to mobile (if text)
                '''

                curr_clipboard_txt = pyperclip.paste()
                if clipboard_state and curr_clipboard_txt != last_clipboard_txt and curr_clipboard_txt != last_clipboard_txt_recieved and curr_clipboard_txt != "":
                    last_clipboard_txt = curr_clipboard_txt
                    data = TEXT_DATA + encode_data(last_clipboard_txt.encode())
                    conn.sendall(data)

                ready, _, _ = select.select([server],[],[],5)
                if ready:
                  rec_data= read(conn)
                  rec_data = decode_data(rec_data)

                  if dtype == "text" and clipboard_state:
                    last_clipboard_txt_recieved = rec_data
                    pyperclip.paste()
                  if dtype == "image":
                     '''
                     if image store it in convert it to image and store it in downloads
                     '''
                     handle_image(rec_data)
                  
                      
                      





                    
                

