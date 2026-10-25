/** The longest side a photo keeps: sharp enough to see a leak, small enough for mobile data. */
const MAX_SIDE = 1600;
const QUALITY = 0.82;

/**
 * Re-encodes a camera or gallery photo as JPEG at most 1600 px on its longest side. Re-encoding drops the
 * metadata a phone embeds (including GPS location), so no location leaves the resident's phone.
 */
export async function preparePhoto(file: File): Promise<Blob> {
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
  const width = Math.round(bitmap.width * scale);
  const height = Math.round(bitmap.height * scale);
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  canvas.getContext('2d')!.drawImage(bitmap, 0, 0, width, height);
  bitmap.close();
  return new Promise((resolve, reject) =>
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Could not read the photo'))), 'image/jpeg', QUALITY),
  );
}
