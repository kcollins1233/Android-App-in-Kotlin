import socketio
import eventlet
import io
import base64
from PIL import Image

# Create a Socket.IO server
sio = socketio.Server(async_mode='eventlet', async_handlers=True)

# Wrap with a WSGI application
app = socketio.WSGIApp(sio)

@sio.event
def connect(sid, environ):
    print('server connected to client with session ID =  ', sid)
    # response(sid, 'connected')
    
@sio.event
def disconnect(sid):
    print('disconnect=>SERVER', 'SID: ', sid)
    
@sio.event
def on_message(sid, data):
    # print(data)
    # received_image = Image.open(decode_image(data))
    # received_image.show()
    print('image received from client: ', sid, 'data: ', data)
    sio.emit('response', data)
    
@sio.event
def message(sid, data):#
    sio.send('message', data)
    print('message ', sid, data)
    
def decode_image(data):
    b = base64.b64decode(data)
    print(b)
    image = Image.open(io.BytesIO(b))
    return image
    
if __name__ == '__main__':
    eventlet.wsgi.server(eventlet.listen(('192.168.0.198', 8080)), app)
    
    