import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'

const A4_WIDTH_MM = 210
const A4_HEIGHT_MM = 297

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function waitForNextFrames(frameCount = 2) {
  for (let index = 0; index < frameCount; index += 1) {
    await new Promise<void>((resolve) => {
      window.requestAnimationFrame(() => resolve())
    })
  }
}

async function waitForFonts() {
  if ('fonts' in document) {
    await (document as Document & { fonts: FontFaceSet }).fonts.ready
  }
}

async function waitForImages(root: HTMLElement) {
  const images = Array.from(root.querySelectorAll('img'))
  await Promise.all(
    images.map((image) => {
      if (image.complete) {
        return Promise.resolve()
      }

      return new Promise<void>((resolve) => {
        image.addEventListener('load', () => resolve(), { once: true })
        image.addEventListener('error', () => resolve(), { once: true })
      })
    }),
  )
}

async function waitForStableLayout(root: HTMLElement) {
  await waitForNextFrames(2)
  await waitForFonts()
  await waitForImages(root)
  await delay(120)
  await waitForNextFrames(2)
}

async function waitForPaginatedResume(root: HTMLElement) {
  const maxAttempts = 40

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const hasRenderedPage = Boolean(root.querySelector('.resume-paper'))
    const paginationReady = root.dataset.paginationReady !== 'false'

    if (hasRenderedPage && paginationReady) {
      return
    }

    await waitForNextFrames(1)
    await delay(40)
  }
}

function buildPageCanvas(sourceCanvas: HTMLCanvasElement, offsetY: number, sliceHeight: number) {
  const pageCanvas = document.createElement('canvas')
  pageCanvas.width = sourceCanvas.width
  pageCanvas.height = sliceHeight

  const context = pageCanvas.getContext('2d')
  if (!context) {
    throw new Error('PDF 导出失败，无法创建画布上下文。')
  }

  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, pageCanvas.width, pageCanvas.height)
  context.drawImage(
    sourceCanvas,
    0,
    offsetY,
    sourceCanvas.width,
    sliceHeight,
    0,
    0,
    pageCanvas.width,
    pageCanvas.height,
  )

  return pageCanvas
}

export async function exportResumeElementToPdf(root: HTMLElement, filename: string) {
  await waitForPaginatedResume(root)
  await waitForStableLayout(root)

  const renderWidth = Math.ceil(root.scrollWidth)
  const renderHeight = Math.ceil(root.scrollHeight)

  if (!renderWidth || !renderHeight) {
    throw new Error('PDF 导出失败，简历内容尚未准备好。')
  }

  const canvas = await html2canvas(root, {
    backgroundColor: '#ffffff',
    width: renderWidth,
    height: renderHeight,
    windowWidth: Math.max(renderWidth, window.innerWidth),
    windowHeight: Math.max(renderHeight, window.innerHeight),
    scale: Math.max(2, Math.min(window.devicePixelRatio || 1, 3)),
    useCORS: true,
    logging: false,
    imageTimeout: 15000,
    scrollX: 0,
    scrollY: -window.scrollY,
  })

  if (!canvas.width || !canvas.height) {
    throw new Error('PDF 导出失败，简历内容渲染为空白。')
  }

  const pdf = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
    compress: true,
  })

  const pageHeightInPixels = Math.floor((canvas.width * A4_HEIGHT_MM) / A4_WIDTH_MM)
  let offsetY = 0
  let pageIndex = 0

  while (offsetY < canvas.height) {
    const sliceHeight = Math.min(pageHeightInPixels, canvas.height - offsetY)
    const pageCanvas = buildPageCanvas(canvas, offsetY, sliceHeight)
    const imageData = pageCanvas.toDataURL('image/jpeg', 0.98)
    const renderedHeight = (sliceHeight * A4_WIDTH_MM) / canvas.width

    if (pageIndex > 0) {
      pdf.addPage()
    }

    pdf.addImage(imageData, 'JPEG', 0, 0, A4_WIDTH_MM, renderedHeight, undefined, 'FAST')
    offsetY += sliceHeight
    pageIndex += 1
  }

  pdf.save(filename)
}
