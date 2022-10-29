import React, { useCallback, useEffect, useState } from 'react';
import {
    Alert, Button, Col, CustomInput, Form, FormFeedback, FormGroup, Input, Label, Pagination, PaginationItem,
    PaginationLink, Row, Table
} from 'reactstrap';
import { Link, useHistory, useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import axios from 'axios';
import _ from 'lodash';

export const Users = ({ jwt }) => {
    const [isLoading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [q, setQ] = useState(null);
    const [data, setData] = useState({ data: [], page, total: 0 });
    const reload = (page, q) => {
        setLoading(true);
        const params = { page, q };
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/users', { headers: { 'Authorization': `Bearer ${jwt}` }, params })
            .then(({ data }) => {
                setData(data);
            })
            .catch(() => {})
            .then(() => {
                setLoading(false)
            })
    };
    const seekTo = (e, to) => {
        e.preventDefault();
        if (to < 1) {
            to = 1
        }

        setPage(to)
    };
    const debouncedReload = useCallback(_.debounce((page, q) => reload(page, q), 250), []);
    useEffect(() => {
        debouncedReload(page, q)
    }, [q, page]);
    // noinspection EqualityComparisonWithCoercionJS
    return (
        <div>
            <h1>Users</h1>
            <hr />
            <Form className="form-inline mb-3" onSubmit={(e) => e.preventDefault()}>
                <Input name="q" placeholder="Search…" type="search" value={q} onChange={e => setQ(e.target.value)} />
            </Form>
            {isLoading ? (
                <p className="text-center">
                    <i className="fas fa-sync fa-spin mr-1" /> Loading&hellip;
                </p>
            ) : (
                <div>
                    <div className="table-responsive mb-3">
                        <Table bordered className="mb-0">
                            <thead className="thead-light">
                            <tr>
                                <th>#</th>
                                <th />
                                <th>Name</th>
                                <th>Username</th>
                                <th>Verified</th>
                                <th>Date created</th>
                                <th />
                            </tr>
                            </thead>
                            <tbody>
                            {data.data.length > 0 ? data.data.map(item => (
                                <tr>
                                    <td>{item.id}</td>
                                    {item.photo ? <td className="text-center"><img alt="" height="32" src={item.photo} /></td> : <td />}
                                    <td>
                                        {item.role === 'admin' ? <strong>{item.name}</strong> : item.name}
                                    </td>
                                    <td>{item.role === 'admin' ? <strong>@{item.username}</strong> : <span>@{item.username}</span>}</td>
                                    <td className={item.verified == 1 ? 'text-success' : 'text-danger'}>
                                        {item.verified == 1 ? 'Yes' : 'No'}
                                    </td>
                                    <td>{item.date_created}</td>
                                    {item.role === 'admin' ? <td /> : (
                                        <td>
                                            <Button color="info" size="sm" tag={Link} to={`/users/${item.id}/edit`}>Edit</Button>
                                            <Button color="danger" className="ml-1" size="sm" tag={Link} to={`/users/${item.id}/delete`}>Delete</Button>
                                        </td>
                                    )}
                                </tr>
                            )) : (
                                <tr><td className="text-muted text-center" colSpan="8">No users found.</td></tr>
                            )}
                            </tbody>
                        </Table>
                    </div>
                </div>
            )}
            <p className="text-center text-lg-left">
                Showing {data.data.length} of {data.total} users (page {data.page} of {data.pages}).
            </p>
            <Pagination>
                <PaginationItem disabled={data.page <= 1}>
                    <PaginationLink href="" onClick={e => seekTo(e, 1)}>&laquo; First</PaginationLink>
                </PaginationItem>
                <PaginationItem disabled={data.page <= 1}>
                    <PaginationLink href="" onClick={e => seekTo(e, data.page - 1)}>Previous</PaginationLink>
                </PaginationItem>
                <PaginationItem disabled={data.page >= data.pages}>
                    <PaginationLink href="" onClick={e => seekTo(e, data.page + 1)}>Next</PaginationLink>
                </PaginationItem>
                <PaginationItem disabled={data.page >= data.pages}>
                    <PaginationLink href="" onClick={e => seekTo(e, data.pages)}>Last &raquo;</PaginationLink>
                </PaginationItem>
            </Pagination>
        </div>
    )
};

Users.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const UsersEdit = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [isSaving, setSaving] = useState(false);
    const [user, setUser] = useState(null);
    const [username, setUsername] = useState(null);
    const [password, setPassword] = useState(null);
    const [name, setName] = useState(null);
    const [email, setEmail] = useState(null);
    const [isVerified, setVerified] = useState(false);
    const [errors, setErrors] = useState({});
    const handleSubmit = e => {
        e.preventDefault();
        setErrors({});
        setSaving(true);
        const data = { name, email, username, password, verified: isVerified };
        axios.put(process.env.REACT_APP_BASE_URL + `/api/admin/users/${id}`, data, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/users')
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
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/users/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setUsername(data.username);
                setName(data.name);
                setEmail(data.email);
                // noinspection EqualityComparisonWithCoercionJS
                setVerified(data.verified == 1);
                setUser(data);
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get user data.
            </p>
        )
    } else if (user) {
        return (
            <div>
                <h1>Users &raquo; Edit</h1>
                <hr />
                <Row>
                    <Col lg={10} xl={8}>
                        <Form onSubmit={handleSubmit}>
                            <FormGroup row>
                                <Label for="user-name" md={3}>Name <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="name" id="user-name" invalid={errors.hasOwnProperty('name')} value={name} required onChange={e => setName(e.target.value)} />
                                    {errors.hasOwnProperty('name') ? <FormFeedback valid={false}>{Object.values(errors['name'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="user-email" md={3}>Email</Label>
                                <Col md={9}>
                                    <Input type="email" name="email" id="user-email" invalid={errors.hasOwnProperty('email')} value={email} onChange={e => setEmail(e.target.value)} />
                                    {errors.hasOwnProperty('email') ? <FormFeedback valid={false}>{Object.values(errors['email'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="user-username" md={3}>Username <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="username" id="user-username" invalid={errors.hasOwnProperty('username')} value={username} required onChange={e => setUsername(e.target.value)} />
                                    {errors.hasOwnProperty('username') ? <FormFeedback valid={false}>{Object.values(errors['username'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="user-password" md={3}>Password</Label>
                                <Col md={9}>
                                    <Input type="password" name="password" id="user-password" invalid={errors.hasOwnProperty('password')} value={password} onChange={e => setPassword(e.target.value)} />
                                    {errors.hasOwnProperty('password') ? <FormFeedback valid={false}>{Object.values(errors['password'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="user-verified" md={3}>Verified</Label>
                                <Col md={9}>
                                    <CustomInput className="mt-md-2" type="switch" id="user-verified" name="verified" checked={isVerified} onChange={e => setVerified(e.target.checked)} label="Yes" />
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

UsersEdit.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const UsersDelete = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isDeleting, setDeleting] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [user, setUser] = useState(null);
    const handleCancel = () => history.push('/users');
    const handleDelete = () => {
        setDeleting(true);
        axios.delete(process.env.REACT_APP_BASE_URL + `/api/admin/users/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/users')
            })
            .catch(() => {})
            .then(() => {
                setDeleting(false)
            })
    };
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/users/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setUser(data);
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get user data.
            </p>
        )
    } else if (user) {
        return (
            <div>
                <h1>Users &raquo; Delete</h1>
                <hr />
                <Alert className="p-3" color="danger">
                    <h4 className="alert-heading">Confirm</h4>
                    <p>
                        You are about to delete <strong>@{user.username}</strong> user which will also delete their videos, comments and likes.
                        Once deleted, it cannot be recovered again.
                        Are you sure?
                    </p>
                    <hr />
                    <Button color="danger" disabled={isDeleting} onClick={handleDelete}>
                        {isDeleting ? (
                            <i className="fas fa-sync fa-spin mr-1" />
                        ) : (
                            <i className="fas fa-trash mr-1" />
                        )}
                        Delete
                    </Button>
                    <Button className="ml-1" color="dark" outline onClick={handleCancel}>Cancel</Button>
                </Alert>
            </div>
        )
    }

    return null
};

UsersDelete.propTypes = {
    jwt: PropTypes.string.isRequired
};
