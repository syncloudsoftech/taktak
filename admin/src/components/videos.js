import React, { useCallback, useEffect, useState } from 'react';
import {
    Alert, Button, Col, CustomInput, Form, FormFeedback, FormGroup, Input, Label, Pagination, PaginationItem,
    PaginationLink, Row, Table
} from 'reactstrap';
import { Link, useHistory, useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import axios from 'axios';
import _ from 'lodash';

export const Videos = ({ jwt }) => {
    const [isLoading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [q, setQ] = useState(null);
    const [data, setData] = useState({ data: [], page, total: 0 });
    const reload = (page, q) => {
        setLoading(true);
        const params = { page, q };
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/videos', { headers: { 'Authorization': `Bearer ${jwt}` }, params })
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
    return (
        <div>
            <h1>Videos</h1>
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
                                <th>Preview</th>
                                <th>User</th>
                                <th>Section</th>
                                <th>Private</th>
                                <th>Date created</th>
                                <th />
                            </tr>
                            </thead>
                            <tbody>
                            {data.data.length > 0 ? data.data.map(item => (
                                <tr>
                                    <td>{item.id}</td>
                                    <td className="text-center">
                                        <a href={item.video} target="_blank">
                                            <img alt="" height="32" src={item.screenshot} />
                                        </a>
                                    </td>
                                    <td>
                                        <Link className="text-body" to={`/users/${item.user_id}/edit`}>
                                            @{item.user_username}
                                        </Link>
                                    </td>
                                    {item.section_id ? <td><Link className="text-body" to={`/video-sections/${item.section_id}/edit`}>{item.section_name}</Link></td> : <td />}
                                    <td className={item.private == 1 ? 'text-success' : 'text-danger'}>
                                        {item.private == 1 ? 'Yes' : 'No'}
                                    </td>
                                    <td>{item.date_created}</td>
                                    <td>
                                        <Button color="info" size="sm" tag={Link} to={`/videos/${item.id}/edit`}>Edit</Button>
                                        <Button color="danger" className="ml-1" size="sm" tag={Link} to={`/videos/${item.id}/delete`}>Delete</Button>
                                    </td>
                                </tr>
                            )) : (
                                <tr><td className="text-muted text-center" colSpan="6">No videos found.</td></tr>
                            )}
                            </tbody>
                        </Table>
                    </div>
                </div>
            )}
            <p className="text-center text-lg-left">
                Showing {data.data.length} of {data.total} videos (page {data.page} of {data.pages}).
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

Videos.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const VideosEdit = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [isSaving, setSaving] = useState(false);
    const [section, setSection] = useState(null);
    const [errors, setErrors] = useState({});
    const [sections, setSections] = useState(null);
    const [video, setVideo] = useState(null);
    const handleSubmit = e => {
        e.preventDefault();
        setErrors({});
        setSaving(true);
        axios.put(process.env.REACT_APP_BASE_URL + `/api/admin/videos/${id}`, { section_id: section }, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/videos')
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
        const call1 = axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/videos/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setSection(data.section_id);
                setVideo(data)
            });
        const call2 = axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/video-sections?count=100', { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setSections(data.data)
            });
        axios.all(process.env.REACT_APP_BASE_URL + [call1, call2])
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get video data.
            </p>
        )
    } else if (video && sections) {
        // noinspection EqualityComparisonWithCoercionJS
        return (
            <div>
                <h1>Videos &raquo; Edit</h1>
                <hr />
                <Row>
                    <Col lg={10} xl={8}>
                        <Form onSubmit={handleSubmit}>
                            <FormGroup row>
                                <Label for="video-section" md={3}>Section</Label>
                                <Col md={9}>
                                    <CustomInput type="select" name="section" id="video-section" invalid={errors.hasOwnProperty('section')} onChange={e => setSection(e.target.value)}>
                                        <option value="">None</option>
                                        {sections.map(section => (
                                            <option value={section.id} selected={section.id == video.section_id}>{section.name}</option>
                                        ))}
                                    </CustomInput>
                                    {errors.hasOwnProperty('section') ? <FormFeedback valid={false}>{Object.values(errors['section'])[0]}</FormFeedback> : null}
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

VideosEdit.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const VideosDelete = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isDeleting, setDeleting] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [video, setVideo] = useState(null);
    const handleCancel = () => history.push('/videos');
    const handleDelete = () => {
        setDeleting(true);
        axios.delete(process.env.REACT_APP_BASE_URL + `/api/admin/videos/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/videos')
            })
            .catch(() => {})
            .then(() => {
                setDeleting(false)
            })
    };
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/videos/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setVideo(data);
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get video data.
            </p>
        )
    } else if (video) {
        return (
            <div>
                <h1>Videos &raquo; Delete</h1>
                <hr />
                <Alert className="p-3" color="danger">
                    <h4 className="alert-heading">Confirm</h4>
                    <p>
                        You are about to delete video <strong>#{video.id}</strong> by <strong>@{video.user_username}</strong>.
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

VideosDelete.propTypes = {
    jwt: PropTypes.string.isRequired
};
