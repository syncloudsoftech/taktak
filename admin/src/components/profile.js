import React, { useEffect, useState } from 'react';
import { Button, Col, Form, FormFeedback, FormGroup, Input, Label, Row } from 'reactstrap';
import PropTypes from 'prop-types';
import axios from 'axios';

export const Profile = ({ jwt }) => {
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [isSaving, setSaving] = useState(false);
    const [self, setSelf] = useState(null);
    const [username, setUsername] = useState(null);
    const [password, setPassword] = useState(null);
    const [name, setName] = useState(null);
    const [email, setEmail] = useState(null);
    const [errors, setErrors] = useState({});
    const handleSubmit = e => {
        e.preventDefault();
        setErrors({});
        setSaving(true);
        const data = { name, email, username, password };
        axios.put(process.env.REACT_APP_BASE_URL + '/api/admin/self', data, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .catch(({ response: { data, status } }) => {
                if (status === 422) {
                    setErrors(data)
                }
            })
            .then(() => {
                setSaving(false)
            })
    };
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/self', { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setUsername(data.username);
                setName(data.name);
                setEmail(data.email);
                setSelf(data);
            })
            .catch(() => {
                setErrored(true)
            })
            .then(() => {
                setLoading(false)
            })
    }, []);
    if (isLoading) {
        return (
            <p className="text-center">
                <i className="fas fa-sync fa-spin mr-1" /> Loading&hellip;
            </p>
        )
    } else if (isErrored) {
        return (
            <p className="text-center">
                <i className="fas fa-times mr-1 text-danger" /> Failed to get profile.
            </p>
        )
    } else if (self) {
        return (
            <div>
                <h1>Profile</h1>
                <hr />
                <Row>
                    <Col lg={10} xl={8}>
                        <Form onSubmit={handleSubmit}>
                            <FormGroup row>
                                <Label for="profile-name" md={3}>Name <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="name" id="profile-name" invalid={errors.hasOwnProperty('name')} value={name} required onChange={e => setName(e.target.value)} />
                                    {errors.hasOwnProperty('name') ? <FormFeedback valid={false}>{Object.values(errors['name'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="profile-email" md={3}>Email</Label>
                                <Col md={9}>
                                    <Input type="email" name="email" id="profile-email" invalid={errors.hasOwnProperty('email')} value={email} onChange={e => setEmail(e.target.value)} />
                                    {errors.hasOwnProperty('email') ? <FormFeedback valid={false}>{Object.values(errors['email'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="profile-username" md={3}>Username <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="username" id="profile-username" invalid={errors.hasOwnProperty('username')} value={username} required onChange={e => setUsername(e.target.value)} />
                                    {errors.hasOwnProperty('username') ? <FormFeedback valid={false}>{Object.values(errors['username'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="profile-password" md={3}>Password</Label>
                                <Col md={9}>
                                    <Input type="password" name="password" id="profile-password" invalid={errors.hasOwnProperty('password')} value={password} onChange={e => setPassword(e.target.value)} />
                                    {errors.hasOwnProperty('password') ? <FormFeedback valid={false}>{Object.values(errors['password'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <Row>
                                <Col md={{offset: 3, size: 9}}>
                                    <Button color="success" disabled={isSaving}>
                                        {isSaving ? (
                                            <i className="fas fa-sync fa-spin mr-1" />
                                        ) : (
                                            <i className="fas fa-check mr-1" />
                                        )}
                                        {' '}
                                        Save
                                    </Button>
                                </Col>
                            </Row>
                        </Form>
                    </Col>
                </Row>
            </div>
        )
    }

    return null
};

Profile.propTypes = {
    jwt: PropTypes.string.isRequired
};
