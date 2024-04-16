import socket
import struct
from PIL import Image
from colorise import colorise
import io

def receive_image():
#Create a socket object
    socket_in = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # Define the host on which you want to connect
    # host = socket.gethostname()

    # Define the port on which you want to connect
    port_in = 8080

    # connect to the server on local computer
    socket_in.bind(("192.168.0.197", port_in))
    socket_in.listen(4)
    print("Listening on port: ", port_in)

    while True:
        client, addr = socket_in.accept()
        print("Connection from: ", addr)
        
        # Receive the size of the byte array
        length = struct.unpack('!i', client.recv(4))[0]
        
        # Receive the byte array
        byte_data = b""
        while len(byte_data) < length:
            byte_data += client.recv(length - len(byte_data))
            
        # Convert the byte array to an image
        image = Image.open(io.BytesIO(byte_data))
        image.save("received_image.bmp")
        client.close()
        colorise("received_image.bmp")
        # send_image()
        
def send_image():
    socket_out = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    port_out = 8181
    socket_out.connect(("192.168.0.199", port_out))
    socket_out.listen(4)
    print("Listening on port: ", port_out)
    
    # Load the image
    image = Image.open("received_image.bmp")
    image.show()
    
    # Convert the image to a byte array
    byte_data = io.BytesIO()
    image.save(byte_data, format="BMP")
    byte_data = byte_data.getvalue()
    
    # Send the size of the byte array
    length = struct.pack('!i', len(byte_data))
    socket_out.send(length)
    
    # Send the byte array
    socket_out.send(byte_data)
    
    # Close the connection
    socket_out.close()

if __name__ == "__main__":
    receive_image()