import { Alert, Button, Form, Input, Segmented, Space, Typography } from "antd";

import { PanelCard } from "./ui/PanelCard";

const { Paragraph, Text } = Typography;

export function AuthPanel({ baseUrl, error, onBaseUrlChange, onSubmit, submitLoading, health }) {
  const [form] = Form.useForm();
  const mode = Form.useWatch("mode", form) || "login";

  function handleFinish(values) {
    onSubmit(values.mode, {
      email: values.email,
      password: values.password
    });
  }

  return (
    <PanelCard
      className="workspace-card auth-entry-card"
      title="Workspace Access"
      extra={<Text type="secondary">Start free, upgrade later</Text>}
    >
      <Space direction="vertical" size={18} className="full-width">
        <Paragraph className="panel-description">
          무료 플랜으로 바로 시작하고, 배치 규모가 커지면 업그레이드하는 SaaS 흐름을 기준으로 구성했습니다. 가입 후 즉시 워크스페이스를 열 수 있습니다.
        </Paragraph>

        <Alert
          type={health.status === "online" ? "success" : health.status === "checking" ? "info" : "warning"}
          showIcon
          message={health.summary}
          description={health.description}
        />

        <Form
          form={form}
          layout="vertical"
          initialValues={{ mode: "login", email: "", password: "", baseUrl }}
          onFinish={handleFinish}
        >
          <Form.Item name="mode" label="Access Mode">
            <Segmented
              block
              options={[
                { label: "Login", value: "login" },
                { label: "Sign Up", value: "signup" }
              ]}
            />
          </Form.Item>

          <Form.Item label="Backend URL">
            <Input value={baseUrl} onChange={(event) => onBaseUrlChange(event.target.value)} />
          </Form.Item>

          <Form.Item
            name="email"
            label="Email"
            rules={[{ required: true, message: "이메일을 입력해주세요." }]}
          >
            <Input placeholder="brand@store.com" />
          </Form.Item>

          <Form.Item
            name="password"
            label="Password"
            rules={[{ required: true, message: "비밀번호를 입력해주세요." }]}
          >
            <Input.Password placeholder="8자 이상 권장" />
          </Form.Item>

          <Button type="primary" htmlType="submit" size="large" block loading={submitLoading} disabled={health.status === "offline"}>
            {mode === "login" ? "Open Seller Workspace" : "Create Seller Account"}
          </Button>
        </Form>

        <div className="auth-benefit-grid">
          <div className="benefit-card">
            <strong>Plan-aware access</strong>
            <span>가입 후 바로 시작하고, 이후 플랜 업그레이드 흐름으로 확장할 수 있습니다.</span>
          </div>
          <div className="benefit-card">
            <strong>Workspace continuity</strong>
            <span>로그인 기반으로 작업 이력과 다운로드 결과를 계속 이어서 관리합니다.</span>
          </div>
        </div>

        {error ? <Alert type="error" showIcon message={error} /> : null}
      </Space>
    </PanelCard>
  );
}
