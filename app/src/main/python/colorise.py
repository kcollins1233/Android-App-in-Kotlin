from deoldify import device
from deoldify.device_id import DeviceId
from deoldify.visualize import *
torch.backends.cudnn.benchmark = True

def colorise(path):
    device.set(device=DeviceId.CPU)

    coloriser = get_image_colorizer(artistic=True)
    img_path = path
    img_out = coloriser.get_transformed_image(path=img_path, render_factor=15, watermarked=True)
    img_out.save(img_path.replace('.bmp', '_colorized.bmp'))
    img_path = 'Done'
    return img_path 