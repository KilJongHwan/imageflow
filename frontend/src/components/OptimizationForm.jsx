import { Alert, Button, Col, Form, InputNumber, Row, Select, Space, Typography, Upload } from "antd";
import { InboxOutlined } from "@ant-design/icons";

import { CropSelector } from "./CropSelector";
import { MarketplacePresetStrip } from "./MarketplacePresetStrip";
import { UploadQueue } from "./UploadQueue";
import { WatermarkStudio } from "./WatermarkStudio";
import { PanelCard } from "./ui/PanelCard";

const { Dragger } = Upload;
const { Paragraph, Text } = Typography;

const aspectRatioOptions = [
  { value: "original", label: "Original" },
  { value: "1:1", label: "1:1" },
  { value: "4:5", label: "4:5" },
  { value: "3:4", label: "3:4" },
  { value: "16:9", label: "16:9" },
  { value: "9:16", label: "9:16" }
];

const cropModeOptions = [
  { value: "fit", label: "Fit Inside" },
  { value: "center-crop", label: "Center Crop" },
  { value: "manual", label: "Manual Region" }
];

export function OptimizationForm({
  files,
  options,
  error,
  statusMessage,
  user,
  submitLoading,
  health,
  onFileChange,
  onFileRemove,
  onFilesClear,
  onOptionsChange,
  onWatermarkImageChange,
  onSubmit
}) {
  const acceptedFileTypes = ".jpg,.jpeg,.png,.webp,.zip";

  function updateOption(key, value) {
    onOptionsChange((current) => {
      if (key === "cropMode" && value !== "manual") {
        return {
          ...current,
          cropMode: value,
          cropX: "",
          cropY: "",
          cropWidth: "",
          cropHeight: ""
        };
      }

      return {
        ...current,
        presetId: key === "presetId" ? value : current.presetId,
        [key]: value ?? ""
      };
    });
  }

  function applyPreset(nextPreset) {
    onOptionsChange((current) => ({
      ...current,
      ...nextPreset,
      cropX: "",
      cropY: "",
      cropWidth: "",
      cropHeight: ""
    }));
  }

  function handleAntUploadChange(info) {
    const nextFiles = info.fileList
      .map((entry) => entry.originFileObj)
      .filter(Boolean);
    onFileChange(nextFiles);
  }

  const uploadFileList = files.map((file) => ({
    uid: `${file.name}-${file.size}-${file.lastModified}`,
    name: file.name,
    status: "done",
    size: file.size,
    type: file.type,
    originFileObj: file
  }));
  const fileCountLabel = `${files.length} file${files.length === 1 ? "" : "s"} ready`;

  return (
    <Space direction="vertical" size={18} className="full-width">
      <PanelCard
        id="workspace-uploader"
        className="workspace-card"
        title="Seller Workspace"
        extra={<Text type="secondary">{user.email}</Text>}
      >
        <Paragraph className="panel-description">
          셀러가 실제로 쓰는 작업실처럼, 채널 프리셋을 고른 뒤 작업 파일을 모으고, 출력 규칙을 조정한 다음 바로 배치를 처리하는 흐름으로 구성했습니다.
        </Paragraph>

        <Alert
          type={health.status === "online" ? "success" : health.status === "checking" ? "info" : "warning"}
          showIcon
          message={health.summary}
          description={health.status === "online"
            ? `현재 ${health.processingMode || "sync"} 모드로 연결되어 있으며, 한 번에 ${health.maxBatchSize || "-"}개까지 처리할 수 있습니다.`
            : health.description}
        />

        <Space direction="vertical" size={18} className="full-width">
          <div>
            <Text strong>Marketplace Presets</Text>
            <Paragraph type="secondary">
              마켓별 대표 규격에서 시작하고, 필요한 경우 수동으로 세부 옵션을 조정하세요.
            </Paragraph>
            <MarketplacePresetStrip activePresetId={options.presetId} onApplyPreset={applyPreset} />
          </div>

          <div>
            <Text strong>Upload Dock</Text>
            <Paragraph type="secondary">
              이미지와 ZIP 파일을 대량으로 올리고, 선택된 파일 목록을 정리한 뒤 같은 규칙으로 한 번에 처리할 수 있습니다.
            </Paragraph>
            <Dragger
              multiple
              accept={acceptedFileTypes}
              showUploadList={false}
              beforeUpload={() => false}
              fileList={uploadFileList}
              onChange={handleAntUploadChange}
              className="workspace-dragger"
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">파일을 끌어다 놓거나 클릭해서 선택하세요</p>
              <p className="ant-upload-hint">
                페이지 전체 드래그 드롭도 지원합니다. 지원 확장자는 jpg, jpeg, png, webp, zip 입니다.
              </p>
            </Dragger>
          </div>

          <UploadQueue files={files} onRemove={onFileRemove} onClear={onFilesClear} />
        </Space>
      </PanelCard>

      <WatermarkStudio
        options={options}
        onWatermarkImageChange={onWatermarkImageChange}
        onOptionsChange={onOptionsChange}
      />

      <PanelCard className="workspace-card" title="Output Rules" extra={<Text type="secondary">Compression, crop, watermark</Text>}>
        <Form layout="vertical" onFinish={onSubmit}>
          <Row gutter={[16, 0]}>
            <Col xs={24} md={8}>
              <Form.Item label="Width">
                <InputNumber value={Number(options.width || 0) || null} onChange={(value) => updateOption("width", value ? String(value) : "")} className="full-width" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Height">
                <InputNumber value={Number(options.height || 0) || null} onChange={(value) => updateOption("height", value ? String(value) : "")} className="full-width" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Quality">
                <InputNumber min={1} max={100} value={Number(options.quality || 0) || null} onChange={(value) => updateOption("quality", value ? String(value) : "")} className="full-width" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={[16, 0]}>
            <Col xs={24} md={8}>
              <Form.Item label="Aspect Ratio">
                <Select value={options.aspectRatio} options={aspectRatioOptions} onChange={(value) => updateOption("aspectRatio", value)} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Crop Mode">
                <Select value={options.cropMode} options={cropModeOptions} onChange={(value) => updateOption("cropMode", value)} />
              </Form.Item>
            </Col>
          </Row>

          {options.cropMode === "manual" && files.length === 1 ? (
            <div className="crop-card-shell">
              <Text strong>Focal Crop Review</Text>
              <Paragraph type="secondary">
                상품이 실제로 보여야 하는 범위를 직접 드래그해서 지정하세요.
              </Paragraph>
              <CropSelector file={files[0]} options={options} onOptionsChange={onOptionsChange} />
            </div>
          ) : null}

          {options.cropMode === "manual" && files.length > 1 ? (
            <Alert
              type="warning"
              showIcon
              message="수동 크롭은 한 장 업로드에서만 사용할 수 있습니다."
              description="여러 장을 동시에 처리할 때는 Center Crop 또는 Fit Inside를 사용하는 편이 안전합니다."
            />
          ) : null}

          <div className="rule-footer">
              <div className="rule-footer-copy">
                <Text strong>Current intent</Text>
                <Paragraph type="secondary">
                  {options.height ? `${options.width} x ${options.height}` : `${options.width || "Auto"} width`} / Q{options.quality || "-"} / {options.cropMode} / {options.watermarkEnabled ? `${options.watermarkStyle || "basic watermark"} on` : "watermark off"}
                </Paragraph>
            </div>
            <div className="rule-footer-actions">
              <Button type="primary" htmlType="submit" size="large" loading={submitLoading} disabled={health.status !== "online"}>
                {submitLoading ? "Processing Batch" : "Optimize Current Batch"}
              </Button>
              <Text type="secondary">{fileCountLabel}</Text>
            </div>
          </div>
        </Form>
      </PanelCard>

      <Space direction="vertical" size={12} className="full-width">
        <Alert type="info" showIcon message={statusMessage} />
        {error ? <Alert type="error" showIcon message={error} /> : null}
      </Space>
    </Space>
  );
}
