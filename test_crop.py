from PIL import Image, ImageChops
import numpy as np

def make_transparent_and_crop(image_path, out_path):
    img = Image.open(image_path).convert("RGBA")
    
    # Make white transparent
    data = np.array(img)
    r, g, b, a = data.T
    white_areas = (r > 240) & (g > 240) & (b > 240)
    data[..., 3][white_areas.T] = 0
    img = Image.fromarray(data)
    
    # Crop to bounding box
    bg = Image.new(img.mode, img.size, (255,255,255,0))
    diff = ImageChops.difference(img, bg)
    diff = ImageChops.add(diff, diff, 2.0, -100)
    bbox = diff.getbbox()
    if bbox:
        img = img.crop(bbox)
        
    img.save(out_path, "PNG")

make_transparent_and_crop('/home/ar3ac/projects/c4rds/app/src/main/res/drawable/ic_brand_esselunga.png', '/home/ar3ac/projects/c4rds/app/src/main/res/drawable/ic_brand_esselunga_cropped.png')
