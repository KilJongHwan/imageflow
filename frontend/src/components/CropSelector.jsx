import { useEffect, useMemo, useRef, useState } from "react";

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function normalizeRect(startX, startY, currentX, currentY) {
  const left = Math.min(startX, currentX);
  const top = Math.min(startY, currentY);
  const width = Math.abs(currentX - startX);
  const height = Math.abs(currentY - startY);
  return { left, top, width, height };
}

export function CropSelector({ file, options, onOptionsChange }) {
  const containerRef = useRef(null);
  const imageRef = useRef(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const [displayRect, setDisplayRect] = useState(null);
  const [dragState, setDragState] = useState(null);
  const [imageMetrics, setImageMetrics] = useState(null);

  useEffect(() => {
    if (!file) {
      setPreviewUrl("");
      setDisplayRect(null);
      setImageMetrics(null);
      return undefined;
    }

    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  useEffect(() => {
    if (!imageMetrics || options.cropMode !== "manual") {
      return;
    }

    if (!options.cropWidth || !options.cropHeight) {
      setDisplayRect(null);
      return;
    }

    const scaleX = imageMetrics.displayWidth / imageMetrics.naturalWidth;
    const scaleY = imageMetrics.displayHeight / imageMetrics.naturalHeight;
    setDisplayRect({
      left: Number(options.cropX || 0) * scaleX,
      top: Number(options.cropY || 0) * scaleY,
      width: Number(options.cropWidth || 0) * scaleX,
      height: Number(options.cropHeight || 0) * scaleY
    });
  }, [imageMetrics, options.cropMode, options.cropX, options.cropY, options.cropWidth, options.cropHeight]);

  const cropSummary = useMemo(() => {
    if (!options.cropWidth || !options.cropHeight) {
      return "드래그해서 잘라낼 범위를 선택하세요.";
    }
    return `선택 영역: x ${options.cropX}, y ${options.cropY}, width ${options.cropWidth}, height ${options.cropHeight}`;
  }, [options.cropX, options.cropY, options.cropWidth, options.cropHeight]);

  function updateMetrics() {
    if (!imageRef.current) {
      return;
    }

    setImageMetrics({
      naturalWidth: imageRef.current.naturalWidth,
      naturalHeight: imageRef.current.naturalHeight,
      displayWidth: imageRef.current.clientWidth,
      displayHeight: imageRef.current.clientHeight
    });
  }

  function beginSelection(event) {
    if (!containerRef.current || !imageMetrics) {
      return;
    }

    const bounds = containerRef.current.getBoundingClientRect();
    const x = clamp(event.clientX - bounds.left, 0, bounds.width);
    const y = clamp(event.clientY - bounds.top, 0, bounds.height);

    event.currentTarget.setPointerCapture(event.pointerId);
    setDragState({ startX: x, startY: y });
    setDisplayRect({ left: x, top: y, width: 0, height: 0 });
  }

  function moveSelection(event) {
    if (!dragState || !containerRef.current) {
      return;
    }

    const bounds = containerRef.current.getBoundingClientRect();
    const x = clamp(event.clientX - bounds.left, 0, bounds.width);
    const y = clamp(event.clientY - bounds.top, 0, bounds.height);
    setDisplayRect(normalizeRect(dragState.startX, dragState.startY, x, y));
  }

  function endSelection() {
    if (!dragState || !displayRect || !imageMetrics) {
      setDragState(null);
      return;
    }

    const scaleX = imageMetrics.naturalWidth / imageMetrics.displayWidth;
    const scaleY = imageMetrics.naturalHeight / imageMetrics.displayHeight;

    const nextCrop = {
      cropX: String(Math.round(displayRect.left * scaleX)),
      cropY: String(Math.round(displayRect.top * scaleY)),
      cropWidth: String(Math.round(displayRect.width * scaleX)),
      cropHeight: String(Math.round(displayRect.height * scaleY))
    };

    if (Number(nextCrop.cropWidth) <= 0 || Number(nextCrop.cropHeight) <= 0) {
      setDragState(null);
      return;
    }

    onOptionsChange((current) => ({
      ...current,
      cropMode: "manual",
      ...nextCrop
    }));
    setDragState(null);
  }

  function clearSelection() {
    setDisplayRect(null);
    onOptionsChange((current) => ({
      ...current,
      cropX: "",
      cropY: "",
      cropWidth: "",
      cropHeight: ""
    }));
  }

  if (!file) {
    return (
      <div className="crop-selector-empty">
        이미지를 먼저 올리면 여기서 직접 크롭 범위를 드래그해 지정할 수 있습니다.
      </div>
    );
  }

  return (
    <div className="crop-selector-shell">
      <div className="crop-selector-head">
        <strong>Manual Crop</strong>
        <button type="button" className="ghost-button" onClick={clearSelection}>
          Clear
        </button>
      </div>

      <div
        ref={containerRef}
        className="crop-canvas"
        onPointerDown={beginSelection}
        onPointerMove={moveSelection}
        onPointerUp={endSelection}
        onPointerLeave={() => {
          if (dragState) {
            endSelection();
          }
        }}
      >
        <img
          ref={imageRef}
          className="crop-source-image"
          src={previewUrl}
          alt="crop source"
          onLoad={updateMetrics}
        />
        {displayRect ? (
          <div
            className="crop-selection"
            style={{
              left: `${displayRect.left}px`,
              top: `${displayRect.top}px`,
              width: `${displayRect.width}px`,
              height: `${displayRect.height}px`
            }}
          />
        ) : null}
      </div>

      <p className="crop-summary">{cropSummary}</p>
    </div>
  );
}
