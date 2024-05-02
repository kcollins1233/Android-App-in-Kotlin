from super_image import EdsrModel, ImageLoader
from PIL import Image




def upscale_image(imageToUpscale):

    image = Image.open(imageToUpscale)
    image = image.resize((1920, 1080), Image.BICUBIC)
    
    print("Upscaling image...")
    # image.show(imageToUpscale)

    model = EdsrModel.from_pretrained('eugenesiow/edsr-base', scale = 2)

    inputs = ImageLoader.load_image(image)
    preds = model(inputs)

    ImageLoader.save_image(preds, 'upscaled_image.bmp')
    # ImageLoader.save_compare(inputs, preds, 'scaled_2x_compare.png')