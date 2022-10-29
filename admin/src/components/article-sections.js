import React, { useCallback, useEffect, useState } from 'react';
import {
    Alert, Button, ButtonToolbar, Col, Form, FormFeedback, FormGroup, Input, Label, Pagination, PaginationItem,
    PaginationLink, Row, Table
} from 'reactstrap';
import { Link, useHistory, useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import axios from 'axios';
import _ from 'lodash';

export const ArticleSections = ({ jwt }) => {
    const [isLoading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [q, setQ] = useState(null);
    const [data, setData] = useState({ data: [], page, total: 0 });
    const reload = (page, q) => {
        setLoading(true);
        const params = { page, q };
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/article-sections', { headers: { 'Authorization': `Bearer ${jwt}` }, params })
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
            <h1>Article sections</h1>
            <hr />
            <Row>
                <div className="col-6">
                    <Form className="form-inline mb-3" onSubmit={(e) => e.preventDefault()}>
                        <Input name="q" placeholder="Search…" type="search" value={q} onChange={e => setQ(e.target.value)} />
                    </Form>
                </div>
                <div className="col-6">
                    <ButtonToolbar>
                        <Link className="btn btn-success ml-auto" to="/article-sections/new">
                            <i className="fas fa-plus mr-1" /> New
                        </Link>
                    </ButtonToolbar>
                </div>
            </Row>
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
                                <th>Name</th>
                                <th>Articles</th>
                                <th>Date created</th>
                                <th />
                            </tr>
                            </thead>
                            <tbody>
                            {data.data.length > 0 ? data.data.map(item => (
                                <tr>
                                    <td>{item.id}</td>
                                    <td>{item.name}</td>
                                    <td>{item.articles}</td>
                                    <td>{item.date_created}</td>
                                    <td>
                                        <Button color="info" size="sm" tag={Link} to={`/article-sections/${item.id}/edit`}>Edit</Button>
                                        <Button color="danger" className="ml-1" size="sm" tag={Link} to={`/article-sections/${item.id}/delete`}>Delete</Button>
                                    </td>
                                </tr>
                            )) : (
                                <tr><td className="text-muted text-center" colSpan="5">No article sections found.</td></tr>
                            )}
                            </tbody>
                        </Table>
                    </div>
                </div>
            )}
            <p className="text-center text-lg-left">
                Showing {data.data.length} of {data.total} article sections (page {data.page} of {data.pages}).
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

ArticleSections.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const ArticleSectionsNew = ({ jwt }) => {
    const history = useHistory();
    const [isSaving, setSaving] = useState(false);
    const [name, setName] = useState(null);
    const [googleNewsTopic, setGoogleNewsTopic] = useState(null);
    const [errors, setErrors] = useState({});
    const handleSubmit = e => {
        e.preventDefault();
        setErrors({});
        setSaving(true);
        const data = {
            name,
            google_news_topic: googleNewsTopic,
        };
        axios.post(process.env.REACT_APP_BASE_URL + '/api/admin/article-sections', data, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/article-sections')
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
            <h1>Article sections &raquo; New</h1>
            <hr />
            <Row>
                <Col lg={10} xl={8}>
                    <Form onSubmit={handleSubmit}>
                        <FormGroup row>
                            <Label for="article-section-name" md={3}>Name <span className="text-danger">*</span></Label>
                            <Col md={9}>
                                <Input name="name" id="article-section-name" invalid={errors.hasOwnProperty('name')} value={name} required onChange={e => setName(e.target.value)} />
                                {errors.hasOwnProperty('name') ? <FormFeedback valid={false}>{Object.values(errors['name'])[0]}</FormFeedback> : null}
                            </Col>
                        </FormGroup>
                        <FormGroup row>
                            <Label for="article-section-google-news-topic" md={3}>Google news topic</Label>
                            <Col md={9}>
                                <Input name="google_news_topic" id="article-section-google-news-topic" invalid={errors.hasOwnProperty('google_news_topic')} value={googleNewsTopic} onChange={e => setGoogleNewsTopic(e.target.value)} />
                                {errors.hasOwnProperty('google_news_topic') ? <FormFeedback valid={false}>{Object.values(errors['google_news_topic'])[0]}</FormFeedback> : null}
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
};

ArticleSectionsNew.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const ArticleSectionsEdit = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [isSaving, setSaving] = useState(false);
    const [name, setName] = useState(null);
    const [googleNewsTopic, setGoogleNewsTopic] = useState(null);
    const [errors, setErrors] = useState({});
    const [section, setSection] = useState(null);
    const handleSubmit = e => {
        e.preventDefault();
        setErrors({});
        setSaving(true);
        const data = {
            name,
            google_news_topic: googleNewsTopic,
        };
        axios.put(process.env.REACT_APP_BASE_URL + `/api/admin/article-sections/${id}`, data, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/article-sections')
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
        axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/article-sections/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setName(data.name);
                setGoogleNewsTopic(data.google_news_topic);
                setSection(data)
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get article section data.
            </p>
        )
    } else if (section) {
        return (
            <div>
                <h1>Article sections &raquo; Edit</h1>
                <hr />
                <Row>
                    <Col lg={10} xl={8}>
                        <Form onSubmit={handleSubmit}>
                            <FormGroup row>
                                <Label for="article-section-name" md={3}>Name <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="name" id="article-section-name" invalid={errors.hasOwnProperty('name')} value={name} required onChange={e => setName(e.target.value)} />
                                    {errors.hasOwnProperty('name') ? <FormFeedback valid={false}>{Object.values(errors['name'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-section-google-news-topic" md={3}>Google news topic</Label>
                                <Col md={9}>
                                    <Input name="google_news_topic" id="article-section-google-news-topic" invalid={errors.hasOwnProperty('google_news_topic')} value={googleNewsTopic} onChange={e => setGoogleNewsTopic(e.target.value)} />
                                    {errors.hasOwnProperty('google_news_topic') ? <FormFeedback valid={false}>{Object.values(errors['google_news_topic'])[0]}</FormFeedback> : null}
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

ArticleSectionsEdit.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const ArticleSectionsDelete = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isDeleting, setDeleting] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [section, setSection] = useState(null);
    const handleCancel = () => history.push('/article-sections');
    const handleDelete = () => {
        setDeleting(true);
        axios.delete(process.env.REACT_APP_BASE_URL + `/api/admin/article-sections/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/article-sections')
            })
            .catch(() => {})
            .then(() => {
                setDeleting(false)
            })
    };
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/article-sections/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setSection(data);
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get article section data.
            </p>
        )
    } else if (section) {
        return (
            <div>
                <h1>Article sections &raquo; Delete</h1>
                <hr />
                <Alert className="p-3" color="danger">
                    <h4 className="alert-heading">Confirm</h4>
                    <p>
                        You are about to delete article section <strong>#{section.id}</strong> which will also delete its <strong>{section.articles} articles</strong>.
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

ArticleSectionsDelete.propTypes = {
    jwt: PropTypes.string.isRequired
};
