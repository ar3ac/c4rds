import os
from PIL import Image, ImageDraw, ImageChops
import numpy as np

def process_logo(image_path, out_path):
    try:
        # 1. Open image and ensure RGBA
        img = Image.open(image_path).convert("RGBA")
        
        # 2. Make exterior white background transparent using Flood Fill
        # Create a new image with a 1-pixel white border to ensure flood fill reaches all edges
        w, h = img.size
        bordered = Image.new("RGBA", (w + 2, h + 2), (255, 255, 255, 255))
        bordered.paste(img, (1, 1))
        
        # We need a mask for flood fill. We use floodfill on a separate mask image.
        # Actually, PIL ImageDraw.floodfill works on the image itself.
        # Let's flood fill starting from (0,0) replacing white (or near white) with transparent.
        # However, ImageDraw.floodfill doesn't support tolerance natively in all versions perfectly for RGBA.
        # A robust way is using skimage or just numpy.
        
        # Simple Numpy Flood Fill from edges (BFS)
        data = np.array(bordered)
        
        # Define what is "white" (R>240, G>240, B>240)
        is_white = (data[:,:,0] > 240) & (data[:,:,1] > 240) & (data[:,:,2] > 240)
        
        # We want to find all white pixels connected to the border (0,0)
        from collections import deque
        
        visited = np.zeros((h+2, w+2), dtype=bool)
        q = deque()
        
        # Add all border pixels to queue if they are white
        for x in range(w+2):
            if is_white[0, x]: q.append((0, x)); visited[0, x] = True
            if is_white[h+1, x]: q.append((h+1, x)); visited[h+1, x] = True
        for y in range(h+2):
            if is_white[y, 0]: q.append((y, 0)); visited[y, 0] = True
            if is_white[y, w+1]: q.append((y, w+1)); visited[y, w+1] = True
            
        while q:
            y, x = q.popleft()
            # Set alpha to 0 for these connected white pixels
            data[y, x, 3] = 0
            
            # check neighbors
            for dy, dx in [(-1,0), (1,0), (0,-1), (0,1)]:
                ny, nx = y+dy, x+dx
                if 0 <= ny < h+2 and 0 <= nx < w+2:
                    if not visited[ny, nx] and is_white[ny, nx]:
                        visited[ny, nx] = True
                        q.append((ny, nx))
                        
        # Extract the original image back from the bordered one
        final_data = data[1:h+1, 1:w+1]
        processed_img = Image.fromarray(final_data)
        
        # 3. Auto-crop to bounding box of non-transparent pixels
        bg = Image.new("RGBA", processed_img.size, (255,255,255,0))
        diff = ImageChops.difference(processed_img, bg)
        bbox = diff.getbbox()
        
        if bbox:
            processed_img = processed_img.crop(bbox)
            
        processed_img.save(out_path, "PNG")
        print(f"Processed {os.path.basename(image_path)} -> Size: {processed_img.size}")
    except Exception as e:
        print(f"Failed {os.path.basename(image_path)}: {e}")

if __name__ == "__main__":
    res_dir = '/home/ar3ac/projects/c4rds/app/src/main/res/drawable/'
    import glob
    for filepath in glob.glob(os.path.join(res_dir, 'ic_brand_*.png')):
        # Skip the cropped ones if any exist from previous runs
        if '_cropped' in filepath: continue
        process_logo(filepath, filepath) # Overwrite
