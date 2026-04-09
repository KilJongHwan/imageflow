import { Button, Empty, Image, List, Space, Tag, Typography } from "antd";
import { DeleteOutlined, FileZipOutlined, FileImageOutlined } from "@ant-design/icons";
import { useEffect, useState } from "react";

const { Text } = Typography;

function formatBytes(value) {
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / (1024 * 1024)).toFixed(2)} MB`;
}

function QueuePreview({ file }) {
  const [previewUrl, setPreviewUrl] = useState("");

  useEffect(() => {
    if (!file.type.startsWith("image/")) {
      setPreviewUrl("");
      return undefined;
    }

    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  if (previewUrl) {
    return <Image src={previewUrl} alt={file.name} width={56} height={56} preview={false} className="queue-image" />;
  }

  return (
    <div className="queue-fallback">
      {file.name.toLowerCase().endsWith(".zip") ? <FileZipOutlined /> : <FileImageOutlined />}
    </div>
  );
}

export function UploadQueue({ files, onRemove, onClear }) {
  if (!files.length) {
    return <Empty description="선택된 파일이 없습니다. 페이지 어디에나 드롭해서 작업 파일을 추가해보세요." />;
  }

  const totalBytes = files.reduce((sum, file) => sum + file.size, 0);
  const archiveCount = files.filter((file) => file.name.toLowerCase().endsWith(".zip")).length;

  return (
    <Space direction="vertical" size={16} className="full-width">
      <div className="queue-toolbar">
        <Space wrap>
          <Tag color="blue">{files.length} files</Tag>
          <Tag>{formatBytes(totalBytes)}</Tag>
          <Tag color={archiveCount ? "gold" : "default"}>{archiveCount} ZIP</Tag>
        </Space>
        <Button type="link" onClick={onClear}>선택 파일 비우기</Button>
      </div>

      <List
        className="upload-queue-list"
        itemLayout="horizontal"
        dataSource={files}
        renderItem={(file) => (
          <List.Item
            key={`${file.name}-${file.size}-${file.lastModified}`}
            actions={[
              <Button
                key="remove"
                danger
                type="text"
                icon={<DeleteOutlined />}
                onClick={() => onRemove(file)}
              >
                Remove
              </Button>
            ]}
          >
            <List.Item.Meta
              avatar={<QueuePreview file={file} />}
              title={<Text strong ellipsis={{ tooltip: file.name }}>{file.name}</Text>}
              description={
                <Space wrap>
                  <Text type="secondary">{formatBytes(file.size)}</Text>
                  <Tag bordered={false} color={file.name.toLowerCase().endsWith(".zip") ? "gold" : "cyan"}>
                    {file.name.toLowerCase().endsWith(".zip") ? "ZIP 묶음" : "이미지 파일"}
                  </Tag>
                </Space>
              }
            />
          </List.Item>
        )}
      />
    </Space>
  );
}
