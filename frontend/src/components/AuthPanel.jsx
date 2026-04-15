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
      className="workspace-card"
      title="Workspace Access"
      extra={<Text type="secondary">JWT protected upload workspace</Text>}
    >
      <Space direction="vertical" size={18} className="full-width">
        <Paragraph className="panel-description">
          먼저 인증을 통과해야 업로드 워크스페이스가 열립니다. 익명 업로드를 막고, 작업 이력과 다운로드를 계정 기준으로 유지합니다.
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
            <strong>Protected uploads</strong>
            <span>업로드와 결과 다운로드가 로그인 세션에 묶입니다.</span>
          </div>
          <div className="benefit-card">
            <strong>Immediate testing</strong>
            <span>회원가입 직후 데모 크레딧으로 흐름을 바로 확인할 수 있습니다.</span>
          </div>
        </div>

        {error ? <Alert type="error" showIcon message={error} /> : null}
      </Space>
    </PanelCard>
  );
}
