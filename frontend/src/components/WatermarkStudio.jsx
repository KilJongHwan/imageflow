import { Alert, Button, Col, Input, Row, Segmented, Skeleton, Slider, Space, Tag, Typography } from "antd";

import { PanelCard } from "./ui/PanelCard";

const { Paragraph, Text } = Typography;

const toneOptions = [
  { label: "Clean", value: "clean" },
  { label: "Premium", value: "premium" },
  { label: "Playful", value: "playful" }
];

const positionLabels = {
  "bottom-right": "Bottom Right",
  "bottom-left": "Bottom Left",
  "top-right": "Top Right",
  center: "Center"
};

function PresetPreview({ preset, selected, onSelect }) {
  return (
    <button
      type="button"
      className={selected ? "watermark-preset-card active" : "watermark-preset-card"}
      onClick={() => onSelect(preset)}
    >
      <div className={`watermark-preview watermark-preview-${preset.style}`}>
        <div className={`watermark-preview-badge is-${preset.position}`}>
          <strong>{preset.brandText}</strong>
          {preset.accentText ? <span>{preset.accentText}</span> : null}
        </div>
      </div>
      <div className="watermark-preset-meta">
        <div>
          <strong>{preset.label}</strong>
          <span>{positionLabels[preset.position] || preset.position}</span>
        </div>
        <Tag color="blue">{preset.tone}</Tag>
      </div>
    </button>
  );
}

export function WatermarkStudio({
  options,
  generateLoading,
  generateError,
  onGenerate,
  onOptionsChange
}) {
  function updateField(key, value) {
    onOptionsChange((current) => ({
      ...current,
      [key]: value ?? ""
    }));
  }

  function handleGenerate() {
    onGenerate({
      brandText: options.watermarkBrandText,
      accentText: options.watermarkAccentText,
      tone: options.watermarkTone
    });
  }

  function handleSelectPreset(preset) {
    onOptionsChange((current) => ({
      ...current,
      watermarkPresetId: preset.id,
      watermarkText: preset.brandText,
      watermarkAccentText: preset.accentText || "",
      watermarkStyle: preset.style,
      watermarkPosition: preset.position,
      watermarkOpacity: String(preset.recommendedOpacity),
      watermarkScalePercent: String(preset.recommendedScalePercent)
    }));
  }

  return (
    <PanelCard className="workspace-card" title="AI Watermark Studio" extra={<Text type="secondary">Generate, choose, apply</Text>}>
      <Space direction="vertical" size={18} className="full-width">
        <Paragraph className="panel-description">
          브랜드명과 톤을 넣으면 커머스용 워터마크 시안을 여러 개 만든 뒤, 원하는 시안을 선택해 현재 배치에 바로 적용할 수 있습니다.
        </Paragraph>

        <Row gutter={[16, 0]}>
          <Col xs={24} md={8}>
            <Text strong>Brand</Text>
            <Input
              value={options.watermarkBrandText}
              placeholder="Lune Atelier"
              onChange={(event) => updateField("watermarkBrandText", event.target.value)}
            />
          </Col>
          <Col xs={24} md={8}>
            <Text strong>Accent Copy</Text>
            <Input
              value={options.watermarkAccentText}
              placeholder="Official Store"
              onChange={(event) => updateField("watermarkAccentText", event.target.value)}
            />
          </Col>
          <Col xs={24} md={8}>
            <Text strong>Tone</Text>
            <Segmented
              block
              value={options.watermarkTone}
              options={toneOptions}
              onChange={(value) => updateField("watermarkTone", value)}
            />
          </Col>
        </Row>

        <div className="watermark-toolbar">
          <Button type="primary" onClick={handleGenerate} loading={generateLoading} disabled={!options.watermarkBrandText.trim()}>
            {generateLoading ? "Generating Presets" : "Generate 3 Presets"}
          </Button>
          <Text type="secondary">선택한 시안은 업로드와 동시에 합성됩니다.</Text>
        </div>

        {generateError ? <Alert type="error" showIcon message={generateError} /> : null}

        {generateLoading ? (
          <Row gutter={[12, 12]}>
            {Array.from({ length: 3 }).map((_, index) => (
              <Col xs={24} md={8} key={index}>
                <Skeleton.Node active className="watermark-skeleton" />
              </Col>
            ))}
          </Row>
        ) : options.watermarkPresets.length > 0 ? (
          <Row gutter={[12, 12]}>
            {options.watermarkPresets.map((preset) => (
              <Col xs={24} md={8} key={preset.id}>
                <PresetPreview preset={preset} selected={options.watermarkPresetId === preset.id} onSelect={handleSelectPreset} />
              </Col>
            ))}
          </Row>
        ) : (
          <Alert
            type="info"
            showIcon
            message="브랜드 워터마크 시안을 아직 생성하지 않았습니다."
            description="브랜드명과 톤을 입력한 뒤 시안을 생성하면 여기서 비교하고 선택할 수 있습니다."
          />
        )}

        <Row gutter={[16, 0]}>
          <Col xs={24} md={8}>
            <Text strong>Position</Text>
            <Segmented
              block
              value={options.watermarkPosition}
              options={[
                { label: "BR", value: "bottom-right" },
                { label: "BL", value: "bottom-left" },
                { label: "TR", value: "top-right" },
                { label: "C", value: "center" }
              ]}
              onChange={(value) => updateField("watermarkPosition", value)}
            />
          </Col>
          <Col xs={24} md={8}>
            <Text strong>Opacity</Text>
            <Slider min={20} max={90} value={Number(options.watermarkOpacity || 56)} onChange={(value) => updateField("watermarkOpacity", String(value))} />
          </Col>
          <Col xs={24} md={8}>
            <Text strong>Scale</Text>
            <Slider min={10} max={40} value={Number(options.watermarkScalePercent || 18)} onChange={(value) => updateField("watermarkScalePercent", String(value))} />
          </Col>
        </Row>
      </Space>
    </PanelCard>
  );
}
