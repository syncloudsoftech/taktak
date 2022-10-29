import React, { useEffect, useState } from 'react';
import { Card, CardBody, CardText, CardTitle, Col, Row } from 'reactstrap';
import PropTypes from 'prop-types';
import axios from 'axios';

export const Dashboard = ({ jwt }) => {
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [data, setData] = useState(null);
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/dashboard', { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setData(data);
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get dashboard data.
            </p>
        )
    } else if (data) {
        return (
            <div>
                <h1>Dashboard</h1>
                <hr />
                <Row>
                    <Col sm={6} lg={3}>
                        <Card className="shadow mb-3 mb-lg-0">
                            <CardBody>
                                <CardTitle tag="h3">{data.users}</CardTitle>
                                <CardText>Users</CardText>
                            </CardBody>
                        </Card>
                    </Col>
                    <Col sm={6} lg={3}>
                        <Card className="shadow mb-3 mb-lg-0">
                            <CardBody>
                                <CardTitle tag="h3">{data.videos}</CardTitle>
                                <CardText>Videos</CardText>
                            </CardBody>
                        </Card>
                    </Col>
                    <Col sm={6} lg={3}>
                        <Card className="shadow mb-3 mb-sm-0">
                            <CardBody>
                                <CardTitle tag="h3">{data.likes}</CardTitle>
                                <CardText>Likes</CardText>
                            </CardBody>
                        </Card>
                    </Col>
                    <Col sm={6} lg={3}>
                        <Card className="shadow">
                            <CardBody>
                                <CardTitle tag="h3">{data.comments}</CardTitle>
                                <CardText>Comments</CardText>
                            </CardBody>
                        </Card>
                    </Col>
                </Row>
            </div>
        )
    }

    return null
};

Dashboard.propTypes = {
    jwt: PropTypes.string.isRequired
};
