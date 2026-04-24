import { useRef, useState } from "react";
import { Alert, Button, Col, Input, Row, Slider, Switch, Typography } from "antd";
import { PanelCard } from "./ui/PanelCard";

const { Text } = Typography;

// ─── constants ────────────────────────────────────────────────────────────────

const POSITIONS = [
  { value: "top-left",     label: "↖ 좌상" },
  { value: "top-right",    label: "↗ 우상" },
  { value: "bottom-left",  label: "↙ 좌하" },
  { value: "bottom-right", label: "↘ 우하" },
];

const FONTS = [
  { value: "sans-serif", label: "Sans" },
  { value: "serif",      label: "Serif" },
  { value: "monospace",  label: "Mono" },
];

const TABS = [
  { key: "text",  label: "텍스트" },
  { key: "image", label: "이미지" },
];

// ─── sub-components ───────────────────────────────────────────────────────────

function SegmentedButtons({ options, value, onChange, block = false }) {
  return (
    <div style={{ display: "flex", gap: 4 }}>
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          style={{
            flex: block ? 1 : undefined,
            padding: "6px 10px",
            fontSize: 12,
            fontWeight: value === opt.value ? 600 : 400,
            border: "1px solid",
            borderColor: value === opt.value ? "#1677ff" : "#d9d9d9",
            borderRadius: 6,
            background: value === opt.value ? "#e6f4ff" : "transparent",
            color: value === opt.value ? "#1677ff" : "#595959",
            cursor: "pointer",
            transition: "all .15s",
            whiteSpace: "nowrap",
          }}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

function SliderRow({ label, min, max, step = 1, value, onChange, format }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
      <span style={{ fontSize: 12, color: "#8c8c8c", minWidth: 52 }}>{label}</span>
      <Slider
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={onChange}
        style={{ flex: 1, margin: 0 }}
      />
      <span style={{ fontSize: 12, fontWeight: 600, minWidth: 36, textAlign: "right", color: "#262626" }}>
        {format ? format(value) : value}
      </span>
    </div>
  );
}

function PositionGrid({ value, onChange }) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 4 }}>
      {POSITIONS.map((p) => (
        <button
          key={p.value}
          type="button"
          onClick={() => onChange(p.value)}
          style={{
            padding: "7px 4px",
            fontSize: 11,
            fontWeight: value === p.value ? 600 : 400,
            border: "1px solid",
            borderColor: value === p.value ? "#1677ff" : "#d9d9d9",
            borderRadius: 6,
            background: value === p.value ? "#e6f4ff" : "transparent",
            color: value === p.value ? "#1677ff" : "#595959",
            cursor: "pointer",
            transition: "all .15s",
          }}
        >
          {p.label}
        </button>
      ))}
    </div>
  );
}

function PreviewBox({ text, font, position, opacity, rotate, size }) {
  const posStyle = {};
  if (position.startsWith("top")) posStyle.top = 12; else posStyle.bottom = 12;
  if (position.endsWith("left")) posStyle.left = 12; else posStyle.right = 12;

  return (
    <div
      style={{
        position: "relative",
        height: 110,
        background: "linear-gradient(135deg, #f0f4f8 0%, #d9e8f5 100%)",
        borderRadius: 8,
        border: "1px solid #e8e8e8",
        overflow: "hidden",
      }}
    >
      {/* fake product image stripes */}
      <div style={{ position: "absolute", inset: 0, opacity: 0.12 }}>
        {[...Array(6)].map((_, i) => (
          <div key={i} style={{
            position: "absolute", left: 0, right: 0,
            top: i * 20, height: 10, background: "#90b8d0",
          }} />
        ))}
      </div>
      <div
        style={{
          position: "absolute",
          ...posStyle,
          fontFamily: font,
          fontSize: size,
          fontWeight: 600,
          opacity: opacity / 100,
          transform: `rotate(${rotate}deg)`,
          color: "#1a1a1a",
          whiteSpace: "nowrap",
          pointerEvents: "none",
          transition: "all .2s",
          letterSpacing: "0.02em",
        }}
      >
        {text || "Watermark"}
      </div>
    </div>
  );
}

// ─── TEXT TAB ─────────────────────────────────────────────────────────────────

function TextTab({ options, onChange }) {
  function update(key, value) {
    onChange((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <Row gutter={[12, 0]}>
        <Col xs={24} md={16}>
          <Label>워터마크 텍스트</Label>
          <Input
            value={options.watermarkText}
            placeholder="브랜드 이름"
            onChange={(e) => update("watermarkText", e.target.value)}
          />
        </Col>
        <Col xs={24} md={8}>
          <Label>폰트</Label>
          <SegmentedButtons
            options={FONTS}
            value={options.watermarkFontFamily || "sans-serif"}
            onChange={(v) => update("watermarkFontFamily", v)}
            block
          />
        </Col>
      </Row>

      <Divider />

      <Row gutter={[16, 12]}>
        <Col xs={24} md={10}>
          <Label>위치</Label>
          <PositionGrid
            value={options.watermarkPosition}
            onChange={(v) => update("watermarkPosition", v)}
          />
        </Col>
        <Col xs={24} md={14}>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <Label>회전</Label>
            <SliderRow
              label="각도"
              min={-45}
              max={45}
              value={Number(options.watermarkRotate || 0)}
              onChange={(v) => update("watermarkRotate", String(v))}
              format={(v) => `${v}°`}
            />
            <Label style={{ marginTop: 4 }}>불투명도 / 크기</Label>
            <SliderRow
              label="불투명도"
              min={10}
              max={100}
              value={Number(options.watermarkOpacity || 56)}
              onChange={(v) => update("watermarkOpacity", String(v))}
              format={(v) => `${v}%`}
            />
            <SliderRow
              label="크기"
              min={8}
              max={48}
              value={Number(options.watermarkTextSize || 16)}
              onChange={(v) => update("watermarkTextSize", String(v))}
              format={(v) => `${v}px`}
            />
          </div>
        </Col>
      </Row>

      <Divider />

      <PreviewBox
        text={options.watermarkText}
        font={options.watermarkFontFamily || "sans-serif"}
        position={options.watermarkPosition || "bottom-right"}
        opacity={Number(options.watermarkOpacity || 56)}
        rotate={Number(options.watermarkRotate || 0)}
        size={Number(options.watermarkTextSize || 16)}
      />
      <span style={{ fontSize: 11, color: "#bfbfbf" }}>실제 합성은 서버에서 처리됩니다.</span>
    </div>
  );
}

// ─── IMAGE TAB ────────────────────────────────────────────────────────────────

function ImageTab({ options, onChange, onImageChange }) {
  const inputRef = useRef(null);

  function update(key, value) {
    onChange((prev) => ({ ...prev, [key]: value }));
  }

  function handleFile(file) {
    if (!file) return;
    onImageChange(file);
  }

  function handleDrop(e) {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      {/* drop zone */}
      <div
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDrop}
        style={{
          border: "1.5px dashed #d9d9d9",
          borderRadius: 8,
          padding: "20px 16px",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 6,
          cursor: "pointer",
          transition: "background .15s",
        }}
        onMouseEnter={(e) => e.currentTarget.style.background = "#fafafa"}
        onMouseLeave={(e) => e.currentTarget.style.background = "transparent"}
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#bfbfbf" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
        <span style={{ fontSize: 13, color: "#8c8c8c" }}>
          {options.watermarkImageFile ? options.watermarkImageFile.name : "클릭하거나 드래그해서 올리기"}
        </span>
        <span style={{ fontSize: 11, color: "#bfbfbf" }}>PNG (투명배경 권장) · JPG · WebP</span>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          style={{ display: "none" }}
          onChange={(e) => handleFile(e.target.files[0])}
        />
      </div>

      {options.watermarkImagePreviewUrl && (
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <img
            src={options.watermarkImagePreviewUrl}
            alt="워터마크 이미지"
            style={{ height: 56, maxWidth: 120, objectFit: "contain", borderRadius: 6, border: "1px solid #e8e8e8" }}
          />
          <Button
            size="small"
            danger
            onClick={() => {
              onImageChange(null);
              if (inputRef.current) inputRef.current.value = "";
            }}
          >
            제거
          </Button>
        </div>
      )}
      
      <Divider />

      <Row gutter={[16, 12]}>
        <Col xs={24} md={10}>
          <Label>위치</Label>
          <PositionGrid
            value={options.watermarkPosition}
            onChange={(v) => update("watermarkPosition", v)}
          />
        </Col>
        <Col xs={24} md={14}>
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <Label>회전 / 불투명도 / 크기</Label>
            <SliderRow
              label="회전"
              min={-45}
              max={45}
              value={Number(options.watermarkRotate || 0)}
              onChange={(v) => update("watermarkRotate", String(v))}
              format={(v) => `${v}°`}
            />
            <SliderRow
              label="불투명도"
              min={10}
              max={100}
              value={Number(options.watermarkOpacity || 80)}
              onChange={(v) => update("watermarkOpacity", String(v))}
              format={(v) => `${v}%`}
            />
            <SliderRow
              label="크기"
              min={5}
              max={50}
              value={Number(options.watermarkScalePercent || 18)}
              onChange={(v) => update("watermarkScalePercent", String(v))}
              format={(v) => `${v}%`}
            />
          </div>
        </Col>
      </Row>
      <span style={{ fontSize: 11, color: "#bfbfbf" }}>실제 합성은 서버에서 처리됩니다.</span>
    </div>
  );
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function Label({ children, style }) {
  return (
    <div style={{ fontSize: 12, fontWeight: 600, color: "#595959", marginBottom: 5, ...style }}>
      {children}
    </div>
  );
}

function Divider() {
  return <div style={{ borderTop: "1px solid #f0f0f0", margin: "0 -2px" }} />;
}

// ─── MAIN EXPORT ──────────────────────────────────────────────────────────────

export function WatermarkStudio({
  options,
  onOptionsChange,
  onWatermarkImageChange,
}) {
  const [activeTab, setActiveTab] = useState("text");

  return (
    <PanelCard
      className="workspace-card"
      title="워터마크"
      extra={
        <div style={{ display: "flex", gap: 2, background: "#f5f5f5", borderRadius: 8, padding: 3 }}>
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              style={{
                padding: "4px 12px",
                fontSize: 12,
                fontWeight: activeTab === tab.key ? 600 : 400,
                border: "none",
                borderRadius: 6,
                background: activeTab === tab.key ? "#fff" : "transparent",
                color: activeTab === tab.key ? "#262626" : "#8c8c8c",
                cursor: "pointer",
                boxShadow: activeTab === tab.key ? "0 1px 3px rgba(0,0,0,.08)" : "none",
                transition: "all .15s",
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
      }
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, marginBottom: 16 }}>
        <div>
          <div style={{ fontSize: 13, fontWeight: 600, color: "#262626" }}>이번 배치에 워터마크 적용</div>
          <div style={{ fontSize: 12, color: "#8c8c8c", marginTop: 2 }}>
            시안을 만들어도 이 스위치를 켠 경우에만 실제 결과물에 들어갑니다.
          </div>
        </div>
        <Switch
          checked={Boolean(options.watermarkEnabled)}
          onChange={(checked) => onOptionsChange((prev) => ({ ...prev, watermarkEnabled: checked }))}
        />
      </div>

      {!options.watermarkEnabled ? (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="워터마크는 현재 미적용 상태입니다."
          description="시안 생성과 설정은 미리 해둘 수 있고, 스위치를 켜야 실제 업로드 배치에 반영됩니다."
        />
      ) : null}

      {activeTab === "text" && (
        <TextTab options={options} onChange={onOptionsChange} />
      )}
      {activeTab === "image" && (
        <ImageTab
          options={options}
          onChange={onOptionsChange}
          onImageChange={onWatermarkImageChange}
        />
      )}
    </PanelCard>
  );
}
