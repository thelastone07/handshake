import socket 
from network.port_info import LOCAL_PORT, get_public_ip
from crypto.keygen import sign_challenge
import pyperclip
import select
import struct
import threading  
import time

NO_OF_CONNECTIONS = 5

last_clipboard_text = ""
last_clipboard_text_rec = ""


def read(conn, stop=b'\x00', buffer_size = 1024):
    """
    Reads data from the connection until the last byte(s) match STOP.
    """
    data = b''
    while True:
        chunk = conn.recv(buffer_size)
        if not chunk:
            break
        data = data + chunk
        while stop in data:
          packet , data = data.split(stop,1)
          if not packet:
             continue
          yield packet

def read_intial(conn, stop=b'\n', buffer_size = 1024):
   
   while True:
      chunk = conn.recv(buffer_size)
      if not chunk:
         break
      return chunk
        
def handle_packet(msg_type:bytes, fmt:bytes, payload:bytes):
  '''
  recives decoded payload
  '''
  global last_clipboard_text_rec
  if msg_type == b'T':
    if fmt == b'txt':
       pyperclip.copy(payload.decode())
       last_clipboard_text_rec = payload.decode()
       
  if msg_type == b'I':
    if fmt == b'png':
      pass
    if fmt == b'jpg':
      pass

def format_msg(data : bytes,msg_type :bytes,fmt :bytes)->bytes:
   header = struct.pack('!c3sI', msg_type, fmt, len(data))
   data = header + data
   return encode_data(data)
      
def send_msg(conn):
   '''
  currently pyperclip works with only text
  if you copy any image or such - it gives you an empty string
  '''
   while True:
    global last_clipboard_text_rec
    global last_clipboard_text
    curr_clipboard_text = pyperclip.paste()
    if curr_clipboard_text != last_clipboard_text and curr_clipboard_text != last_clipboard_text_rec and curr_clipboard_text != "":
        # data = format_msg(curr_clipboard_text.encode(),b'T', b'txt')
        data = curr_clipboard_text.encode()
        print(data)
        conn.sendall(data)
        last_clipboard_text = curr_clipboard_text
    time.sleep(0.5)
      

def receive_msg(sock,conn):
   while True:
      ready, _, _ = select.select([sock], [], [], 1.0)
      if ready:
         for data in read(conn):
          decoded = decode_data(data)
          msg_type, fmt, size = struct.unpack('!c3sI', decoded[:8])
          payload = decoded[8:]
          assert len(payload) == size 
          handle_packet(msg_type, fmt, payload)
    

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




def listen():
    """
    Starts server listening
    """

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    ip = get_public_ip()
    server.bind((f'{ip}', LOCAL_PORT))
    server.listen(NO_OF_CONNECTIONS)

    while True:
      ready, _, _ = select.select([server], [], [], 1.0)
      if ready:
          conn, _ = server.accept()
          
          try:
              data = read_intial(conn)
              parts = data.decode().rstrip('\n').split('|')
              second_part_bytes = bytes.fromhex(parts[1])
              signature = sign_challenge(second_part_bytes)
              
              # Validate signature
              if not isinstance(signature, bytes):
                  raise ValueError("Signature must be bytes")
              if len(signature) != 64:
                  raise ValueError("Signature must be 64 bytes")

              conn.sendall(signature)

              # Start the receive and send threads only once per connection
              recv_thread = threading.Thread(target=receive_msg, args=(server, conn), daemon=True)
              send_thread = threading.Thread(target=send_msg, args=(conn,), daemon=True)

              # recv_thread.start()
              send_thread.start()

              print("threading started")

          except Exception as e:
              print("something happened", e)
              conn.close()
