import { Card } from "antd";

export function PanelCard({ title, extra, children, className, bodyStyle }) {
  return (
    <Card
      title={title}
      extra={extra}
      className={className}
      bordered={false}
      bodyStyle={bodyStyle}
    >
      {children}
    </Card>
  );
}
