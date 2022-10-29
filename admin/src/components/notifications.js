import React, { useState } from 'react';
import { Button, Col, Form, FormFeedback, FormGroup, Input, Label, Row } from 'reactstrap';
import { useHistory } from 'react-router-dom';
import PropTypes from 'prop-types';
import axios from 'axios';

export const NotificationsNew = ({ jwt }) => {
    const history = useHistory();
    const [isSaving, setSaving] = useState(false);
    const [title, setTitle] = useState(null);
    const [body, setBody] = useState(null);
    const [errors, setErrors] = useState({});
    const handleSubmit = e => {
        e.preventDefault();
        setErrors({});
        setSaving(true);
        const data = { title, body };
        axios.post(process.env.REACT_APP_BASE_URL + '/api/admin/notifications', data, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/notifications')
            })
            .catch(({ response: { data, status } }) => {
                if (status === 422) {
                    setErrors(data)
                }
            })
            .then(() => {
                setSaving(false)
            })
    };
    return (
        <div>
            <h1>Notifications &raquo; New</h1>
            <hr />
            <Row>
                <Col lg={10} xl={8}>
                    <Form onSubmit={handleSubmit}>
                        <FormGroup row>
                            <Label for="notification-title" md={3}>Title <span className="text-danger">*</span></Label>
                            <Col md={9}>
                                <Input name="title" id="notification-title" invalid={errors.hasOwnProperty('title')} value={title} required onChange={e => setTitle(e.target.value)} />
                                {errors.hasOwnProperty('title') ? <FormFeedback valid={false}>{Object.values(errors['title'])[0]}</FormFeedback> : null}
                            </Col>
                        </FormGroup>
                        <FormGroup row>
                            <Label for="notification-body" md={3}>Body <span className="text-danger">*</span></Label>
                            <Col md={9}>
                                <Input name="body" id="notification-body" invalid={errors.hasOwnProperty('body')} value={body} required type="textarea" onChange={e => setBody(e.target.value)} />
                                {errors.hasOwnProperty('body') ? <FormFeedback valid={false}>{Object.values(errors['body'])[0]}</FormFeedback> : null}
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
                                    Send
                                </Button>
                            </Col>
                        </Row>
                    </Form>
                </Col>
            </Row>
        </div>
    )
};

NotificationsNew.propTypes = {
    jwt: PropTypes.string.isRequired
};
