import { Card } from "antd";

export function PanelCard({ title, extra, children, className, bodyStyle, ...restProps }) {
  return (
    <Card
      title={title}
      extra={extra}
      className={className}
      bordered={false}
      bodyStyle={bodyStyle}
      {...restProps}
    >
      {children}
    </Card>
  );
}
