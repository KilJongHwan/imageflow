import { Alert, Button, Divider, Form, Input, Segmented, Space, Typography } from "antd";
import { ArrowLeftOutlined } from "@ant-design/icons";

import { PanelCard } from "./ui/PanelCard";

const { Paragraph, Text, Title } = Typography;

const defaultProviders = [
  { provider: "GOOGLE", enabled: false, authUrl: "" },
  { provider: "NAVER", enabled: false, authUrl: "" }
];

export function AuthPage({ error, providers, submitLoading, health, onBack, onSubmit }) {
  const [form] = Form.useForm();
  const mode = Form.useWatch("mode", form) || "login";
  const providerMap = new Map((providers || []).map((provider) => [provider.provider, provider]));
  const visibleProviders = defaultProviders.map((provider) => providerMap.get(provider.provider) || provider);

  function handleFinish(values) {
    onSubmit(values.mode, {
      email: values.email,
      password: values.password
    });
  }

  return (
    <div className="auth-page-shell">
      <div className="auth-page-topbar">
        <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
          Back To Landing
        </Button>
      </div>

      <PanelCard
        className="workspace-card auth-page-card"
        title="Workspace Access"
        extra={<Text type="secondary">Production-style sign in</Text>}
      >
        <Space direction="vertical" size={18} className="full-width">
          <div>
            <Title level={2} className="auth-page-title">로그인 또는 회원가입</Title>
            <Paragraph className="panel-description">
              랜딩과 분리된 전용 인증 화면입니다. 실제 서비스처럼 로그인 후 워크스페이스로 이동하는 흐름으로 구성했습니다.
            </Paragraph>
          </div>

          <Alert
            type={health.status === "online" ? "success" : health.status === "checking" ? "info" : "warning"}
            showIcon
            message={health.summary}
            description={health.description}
          />

          <Form
            form={form}
            layout="vertical"
            initialValues={{ mode: "login", email: "", password: "" }}
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
              {mode === "login" ? "Log In" : "Create Account"}
            </Button>
          </Form>

          <Divider plain>Social Sign-In</Divider>

          <Space direction="vertical" size={10} className="full-width">
            {visibleProviders.map((provider) => (
              <Button
                key={provider.provider}
                block
                href={provider.enabled ? provider.authUrl : undefined}
                disabled={!provider.enabled}
              >
                {provider.enabled ? `Continue with ${provider.provider}` : `${provider.provider} setup required`}
              </Button>
            ))}
          </Space>

          <div className="auth-benefit-grid">
            <div className="benefit-card">
              <strong>Dedicated auth flow</strong>
              <span>랜딩에서 바로 폼을 열지 않고, 인증 전용 화면에서 로그인과 회원가입을 처리합니다.</span>
            </div>
            <div className="benefit-card">
              <strong>Master-ready account model</strong>
              <span>일반 사용자와 master 계정을 구분할 수 있는 구조를 포함합니다.</span>
            </div>
          </div>

          {error ? <Alert type="error" showIcon message={error} /> : null}
        </Space>
      </PanelCard>
    </div>
  );
}
