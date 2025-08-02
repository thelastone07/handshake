import socket
from network.port_info import LOCAL_PORT, get_public_ip
from crypto.keygen import sign_challenge
import time, struct, threading, pyperclip

def read_initial(soc, buffer=1024)->bytes:
    try:
        while True:
            chunk = soc.recv(buffer)
            if not chunk:
                break
            return chunk
    except Exception as e:
        print("reading intial data failed :",e)
    
text = pyperclip.paste()
text_rec = ""

def format_msg(data : bytes,msg_type :bytes,fmt :bytes)->bytes:
   header = struct.pack('!c3sI', msg_type, fmt, len(data))
   data = header + data
   return encode_data(data)

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

def send(soc):
    print('i am listening')
    try:
        while True:
            global text, text_rec
            curr = pyperclip.paste()
            should_send = curr != "" and text != curr and curr != text_rec
            if should_send:
                print("printing data to be sent")
                print(curr)
                message = curr.encode()
                print(message)
                message = format_msg(message, b'T', b'txt')
                print(message)
                soc.sendall(message)
            else:
                time.sleep(0.5)
            text = curr
    except Exception as e:
        print("Error or client disconnected: ", e)
    except KeyboardInterrupt:
        print("no more talking. keyboard has spoken")
    finally:
        print("Closing connection...")
        soc.close()
                


def listen():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    ip = get_public_ip()
    server.bind((f'{ip}', LOCAL_PORT))
    server.listen(1)

    soc, addr = server.accept()

    #receive first message 
    try :
        data = read_initial(soc).decode()
        data = data.rstrip('\n').split('|')[1]
        signature = sign_challenge(bytes.fromhex(data))

        if not isinstance(signature, bytes):
            raise ValueError("sign not bytes")
        if len(signature) != 64:
            raise ValueError("sign not 64 in length")

    except Exception as e:
        print("error while reading initial data :", e)

    #send intial message
    try :
        soc.sendall(signature) 
    except Exception as e:
        print("signature can't send :", e)


    thread = threading.Thread(target=send, args=(soc,))
    thread.start()


