import React, { useState } from 'react';
import { Button, Col, Form, FormFeedback, FormGroup, Input, Label, Row } from 'reactstrap';
import PropTypes from 'prop-types';
import axios from 'axios';

export const Login = ({ onLoginSuccess }) => {
    const [username, setUsername] = useState(null);
    const [password, setPassword] = useState(null);
    const [errors, setErrors] = useState({});
    const [isSubmitting, setSubmitting] = useState(false);
    const handleSubmit = e => {
        e.preventDefault();
        setErrors({});
        setSubmitting(true);
        axios.post(process.env.REACT_APP_BASE_URL + '/api/admin/login', { username, password })
            .then(({ data }) => {
                onLoginSuccess(data.jwt)
            })
            .catch(({ response: { data, status } }) => {
                if (status === 422) {
                    setErrors(data)
                }
            })
            .then(() => {
                setSubmitting(false)
            })
    };
    return (
        <Row className="my-3 my-md-4 my-lg-5">
            <Col sm={{ size: 8, offset: 2 }} md={{ size: 6, offset: 3 }} lg={{ size: 4, offset: 4 }}>
                <Form onSubmit={handleSubmit}>
                    <FormGroup>
                        <Label for="login-username">Username <span className="text-danger">*</span></Label>
                        <Input name="username" id="login-username" invalid={errors.hasOwnProperty('username')} value={username} required onChange={e => setUsername(e.target.value)} />
                        {errors.hasOwnProperty('username') ? <FormFeedback valid={false}>{Object.values(errors['username'])[0]}</FormFeedback> : null}
                    </FormGroup>
                    <FormGroup>
                        <Label for="login-password">Password <span className="text-danger">*</span></Label>
                        <Input type="password" name="password" id="login-password" invalid={errors.hasOwnProperty('password')} value={password} required onChange={e => setPassword(e.target.value)} />
                        {errors.hasOwnProperty('password') ? <FormFeedback valid={false}>{Object.values(errors['password'])[0]}</FormFeedback> : null}
                    </FormGroup>
                    <Button color="success" disabled={isSubmitting}>
                        {isSubmitting ? (
                            <i className="fas fa-sync fa-spin mr-1" />
                        ) : (
                            <i className="fas fa-check mr-1" />
                        )}
                        {' '}
                        Login
                    </Button>
                </Form>
            </Col>
        </Row>
    )
};

Login.propTypes = {
    onLoginSuccess: PropTypes.func.isRequired
};
