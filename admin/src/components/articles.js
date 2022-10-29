import React, { useCallback, useEffect, useState } from 'react';
import {
    Alert, Button, ButtonToolbar, Col, CustomInput, Form, FormFeedback, FormGroup, Input, Label, Pagination,
    PaginationItem, PaginationLink, Row, Table
} from 'reactstrap';
import { Link, useHistory, useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import axios from 'axios';
import _ from 'lodash';

export const Articles = ({ jwt }) => {
    const [isLoading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [q, setQ] = useState(null);
    const [data, setData] = useState({ data: [], page, total: 0 });
    const reload = (page, q) => {
        setLoading(true);
        const params = { page, q };
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/articles', { headers: { 'Authorization': `Bearer ${jwt}` }, params })
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
            <h1>Articles</h1>
            <hr />
            <Row>
                <div className="col-6">
                    <Form className="form-inline mb-3" onSubmit={(e) => e.preventDefault()}>
                        <Input name="q" placeholder="Search…" type="search" value={q} onChange={e => setQ(e.target.value)} />
                    </Form>
                </div>
                <div className="col-6">
                    <ButtonToolbar>
                        <Link className="btn btn-success ml-auto" to="/articles/new">
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
                                <th />
                                <th>Title</th>
                                <th>Publisher</th>
                                <th>Section</th>
                                <th>Date reported</th>
                                <th />
                            </tr>
                            </thead>
                            <tbody>
                            {data.data.length > 0 ? data.data.map(item => (
                                <tr>
                                    <td>{item.id}</td>
                                    {item.image ? <td className="text-center"><img alt="" height="32" src={item.image} /></td> : <td />}
                                    <td>
                                        <a href={item.url} target="_blank">
                                            {item.title.length > 32 ? item.title.substr(0, 32) + '…' : item.title}
                                        </a>
                                    </td>
                                    <td>{item.publisher}</td>
                                    <td>{item.section_name}</td>
                                    <td>{item.date_reported}</td>
                                    <td>
                                        <Button color="info" size="sm" tag={Link} to={`/articles/${item.id}/edit`}>Edit</Button>
                                        <Button color="danger" className="ml-1" size="sm" tag={Link} to={`/articles/${item.id}/delete`}>Delete</Button>
                                    </td>
                                </tr>
                            )) : (
                                <tr><td className="text-muted text-center" colSpan="7">No articles found.</td></tr>
                            )}
                            </tbody>
                        </Table>
                    </div>
                </div>
            )}
            <p className="text-center text-lg-left">
                Showing {data.data.length} of {data.total} articles (page {data.page} of {data.pages}).
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

Articles.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const ArticlesNew = ({ jwt }) => {
    const history = useHistory();
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [isSaving, setSaving] = useState(false);
    const [section, setSection] = useState(null);
    const [title, setTitle] = useState(null);
    const [snippet, setSnippet] = useState(null);
    const [image, setImage] = useState(null);
    const [url, setUrl] = useState(null);
    const [publisher, setPublisher] = useState(null);
    const [dateReported, setDateReported] = useState(null);
    const [errors, setErrors] = useState({});
    const [sections, setSections] = useState(null);
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/article-sections?count=100', { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setSections(data.data)
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get article sections data.
            </p>
        )
    } else if (sections) {
        const handleSubmit = e => {
            e.preventDefault();
            setErrors({});
            setSaving(true);
            const data = {
                section_id: section,
                title,
                snippet,
                image,
                url,
                publisher,
                date_reported: dateReported
            };
            axios.post(process.env.REACT_APP_BASE_URL + '/api/admin/articles', data, { headers: { 'Authorization': `Bearer ${jwt}` } })
                .then(() => {
                    history.push('/articles')
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
                <h1>Articles &raquo; New</h1>
                <hr />
                <Row>
                    <Col lg={10} xl={8}>
                        <Form onSubmit={handleSubmit}>
                            <FormGroup row>
                                <Label for="article-section" md={3}>Section <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <CustomInput type="select" name="section" id="article-section" invalid={errors.hasOwnProperty('section')} required onChange={e => setSection(e.target.value)}>
                                        <option value="">None</option>
                                        {sections.map(section => (
                                            <option value={section.id}>{section.name}</option>
                                        ))}
                                    </CustomInput>
                                    {errors.hasOwnProperty('section') ? <FormFeedback valid={false}>{Object.values(errors['section'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-title" md={3}>Title <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="title" id="article-title" invalid={errors.hasOwnProperty('title')} value={title} required onChange={e => setTitle(e.target.value)} />
                                    {errors.hasOwnProperty('title') ? <FormFeedback valid={false}>{Object.values(errors['title'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-snippet" md={3}>Snippet <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="snippet" id="article-snippet" invalid={errors.hasOwnProperty('snippet')} value={snippet} required type="textarea" onChange={e => setSnippet(e.target.value)} />
                                    {errors.hasOwnProperty('snippet') ? <FormFeedback valid={false}>{Object.values(errors['snippet'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-image" md={3}>Image <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="image" id="article-image" invalid={errors.hasOwnProperty('image')} value={image} required onChange={e => setImage(e.target.value)} />
                                    {errors.hasOwnProperty('image') ? <FormFeedback valid={false}>{Object.values(errors['image'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-url" md={3}>URL <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="url" id="article-url" invalid={errors.hasOwnProperty('url')} value={url} required onChange={e => setUrl(e.target.value)} />
                                    {errors.hasOwnProperty('url') ? <FormFeedback valid={false}>{Object.values(errors['url'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-publisher" md={3}>Publisher</Label>
                                <Col md={9}>
                                    <Input name="publisher" id="article-publisher" invalid={errors.hasOwnProperty('publisher')} value={publisher} onChange={e => setPublisher(e.target.value)} />
                                    {errors.hasOwnProperty('publisher') ? <FormFeedback valid={false}>{Object.values(errors['publisher'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-date-reported" md={3}>Date reported <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="date_reported" id="article-date-reported" invalid={errors.hasOwnProperty('date_reported')} value={dateReported} required onChange={e => setDateReported(e.target.value)} />
                                    {errors.hasOwnProperty('date_reported') ? <FormFeedback valid={false}>{Object.values(errors['date_reported'])[0]}</FormFeedback> : null}
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

ArticlesNew.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const ArticlesEdit = ({ jwt }) => {
    const { id } = useParams();
    const history = useHistory();
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [isSaving, setSaving] = useState(false);
    const [section, setSection] = useState(null);
    const [title, setTitle] = useState(null);
    const [snippet, setSnippet] = useState(null);
    const [image, setImage] = useState(null);
    const [url, setUrl] = useState(null);
    const [publisher, setPublisher] = useState(null);
    const [dateReported, setDateReported] = useState(null);
    const [errors, setErrors] = useState({});
    const [sections, setSections] = useState(null);
    const [article, setArticle] = useState(null);
    useEffect(() => {
        setLoading(true);
        const call1 = axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/articles/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setTitle(data.title);
                setSnippet(data.snippet);
                setImage(data.image);
                setUrl(data.url);
                setPublisher(data.publisher);
                setDateReported(data.date_reported);
                setSection(data.section_id);
                setArticle(data)
            });
        const call2 = axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/article-sections?count=100', { headers: { 'Authorization': `Bearer ${jwt}` } })
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get article sections data.
            </p>
        )
    } else if (article && sections) {
        const handleSubmit = e => {
            e.preventDefault();
            setErrors({});
            setSaving(true);
            const data = {
                section_id: section,
                title,
                snippet,
                image,
                url,
                publisher,
                date_reported: dateReported
            };
            axios.put(process.env.REACT_APP_BASE_URL + `/api/admin/articles/${id}`, data, { headers: { 'Authorization': `Bearer ${jwt}` } })
                .then(() => {
                    history.push('/articles')
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
        // noinspection EqualityComparisonWithCoercionJS
        return (
            <div>
                <h1>Articles &raquo; Edit</h1>
                <hr />
                <Row>
                    <Col lg={10} xl={8}>
                        <Form onSubmit={handleSubmit}>
                            <FormGroup row>
                                <Label for="article-section" md={3}>Section <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <CustomInput type="select" name="section" id="article-section" invalid={errors.hasOwnProperty('section')} required onChange={e => setSection(e.target.value)}>
                                        <option value="">None</option>
                                        {sections.map(section => (
                                            <option value={section.id} selected={section.id == article.section_id}>{section.name}</option>
                                        ))}
                                    </CustomInput>
                                    {errors.hasOwnProperty('section') ? <FormFeedback valid={false}>{Object.values(errors['section'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-title" md={3}>Title <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="title" id="article-title" invalid={errors.hasOwnProperty('title')} value={title} required onChange={e => setTitle(e.target.value)} />
                                    {errors.hasOwnProperty('title') ? <FormFeedback valid={false}>{Object.values(errors['title'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-snippet" md={3}>Snippet <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="snippet" id="article-snippet" invalid={errors.hasOwnProperty('snippet')} value={snippet} required type="textarea" onChange={e => setSnippet(e.target.value)} />
                                    {errors.hasOwnProperty('snippet') ? <FormFeedback valid={false}>{Object.values(errors['snippet'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-image" md={3}>Image <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="image" id="article-image" invalid={errors.hasOwnProperty('image')} value={image} required onChange={e => setImage(e.target.value)} />
                                    {errors.hasOwnProperty('image') ? <FormFeedback valid={false}>{Object.values(errors['image'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-url" md={3}>URL <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="url" id="article-url" invalid={errors.hasOwnProperty('url')} value={url} required onChange={e => setUrl(e.target.value)} />
                                    {errors.hasOwnProperty('url') ? <FormFeedback valid={false}>{Object.values(errors['url'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-publisher" md={3}>Publisher</Label>
                                <Col md={9}>
                                    <Input name="publisher" id="article-publisher" invalid={errors.hasOwnProperty('publisher')} value={publisher} onChange={e => setPublisher(e.target.value)} />
                                    {errors.hasOwnProperty('publisher') ? <FormFeedback valid={false}>{Object.values(errors['publisher'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="article-date-reported" md={3}>Date reported <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="date_reported" id="article-date-reported" invalid={errors.hasOwnProperty('date_reported')} value={dateReported} required onChange={e => setDateReported(e.target.value)} />
                                    {errors.hasOwnProperty('date_reported') ? <FormFeedback valid={false}>{Object.values(errors['date_reported'])[0]}</FormFeedback> : null}
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

ArticlesEdit.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const ArticlesDelete = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isDeleting, setDeleting] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [article, setArticle] = useState(null);
    const handleCancel = () => history.push('/articles');
    const handleDelete = () => {
        setDeleting(true);
        axios.delete(process.env.REACT_APP_BASE_URL + `/api/admin/articles/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/articles')
            })
            .catch(() => {})
            .then(() => {
                setDeleting(false)
            })
    };
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/articles/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setArticle(data);
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get article data.
            </p>
        )
    } else if (article) {
        return (
            <div>
                <h1>Articles &raquo; Delete</h1>
                <hr />
                <Alert className="p-3" color="danger">
                    <h4 className="alert-heading">Confirm</h4>
                    <p>
                        You are about to delete article <strong>{article.title}</strong> permanently.
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

ArticlesDelete.propTypes = {
    jwt: PropTypes.string.isRequired
};
