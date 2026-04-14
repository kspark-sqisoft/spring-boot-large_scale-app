import { postMultipartJson } from '@/shared/api/client'

export type UploadResponse = {
  id: string
  url: string
  contentType: string
}

export function uploadImage(file: File): Promise<UploadResponse> {
  return postMultipartJson<UploadResponse>('/uploads/image', file)
}
