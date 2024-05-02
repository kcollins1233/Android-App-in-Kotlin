import socket
import struct
from PIL import Image
from colourise import colourise
from upscale import upscale_image
import io

def receive_image():
    while True:
        try:
            #Create a socket object
            socket_in = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

            # Define the host on which you want to connect
            # host = socket.gethostname()

            # Define the port on which you want to connect
            port_in = 8080

            # connect to the server on local computer
            socket_in.bind(("192.168.0.194", port_in)) # change to the IP of the server machine
            socket_in.listen(4)
            print("Listening on port: ", port_in)
            
            socket_in.settimeout(10)
            
            client, addr = socket_in.accept()
            print("Connection from: ", addr)
            
            data = client.recv(1024)
            message = data.decode('utf-8')
        
            # Receive the size of the byte array
            length = struct.unpack('!i', client.recv(4))[0]
            
            # Receive the byte array
            byte_data = b""
            while len(byte_data) < length:
                byte_data += client.recv(length - len(byte_data))
                
            # Convert the byte array to an image
            image = Image.open(io.BytesIO(byte_data))
            image.save("received_image.bmp")
            # client.close()
            
            if 'upscale' in message:
                upscale_image("received_image.bmp")
                socket_in.close()
                send_image('upscaled_image')
            elif 'colourise' in message:
                colourise("received_image.bmp")
                socket_in.close()
                send_image('colourised_image')
                
        except socket.timeout:
            print("No connection in 10 seconds")
            socket_in.close()
            

        
def send_image(processed_image):
    socket_out = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    port_out = 8181
    socket_out.connect(("192.168.0.196", port_out)) # change to the IP of the client device that the app is running on
    print("Sending on port: ", port_out)
    
    # Load the image
    if processed_image == 'upscaled_image':
        image = Image.open("upscaled_image.bmp")
    elif processed_image == 'colourised_image':
        image = Image.open("colourised_image.bmp")
    # image.show()
    
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