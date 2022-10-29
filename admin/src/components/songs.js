import React, { useCallback, useEffect, useState } from 'react';
import {
    Alert, Button, ButtonToolbar, Col, CustomInput, Form, FormFeedback, FormGroup, Input, Label, Pagination,
    PaginationItem, PaginationLink, Row, Table
} from 'reactstrap';
import { Link, useHistory, useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import axios from 'axios';
import _ from 'lodash';

export const Songs = ({ jwt }) => {
    const [isLoading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [q, setQ] = useState(null);
    const [data, setData] = useState({ data: [], page, total: 0 });
    const reload = (page, q) => {
        setLoading(true);
        const params = { page, q };
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/songs', { headers: { 'Authorization': `Bearer ${jwt}` }, params })
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
            <h1>Songs</h1>
            <hr />
            <Row>
                <div className="col-6">
                    <Form className="form-inline mb-3" onSubmit={(e) => e.preventDefault()}>
                        <Input name="q" placeholder="Search…" type="search" value={q} onChange={e => setQ(e.target.value)} />
                    </Form>
                </div>
                <div className="col-6">
                    <ButtonToolbar>
                        <Link className="btn btn-success ml-auto" to="/songs/new">
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
                                <th>Name</th>
                                <th>Section</th>
                                <th>Videos</th>
                                <th>Date created</th>
                                <th />
                            </tr>
                            </thead>
                            <tbody>
                            {data.data.length > 0 ? data.data.map(item => (
                                <tr>
                                    <td>{item.id}</td>
                                    {item.icon ? <td className="text-center"><img alt="" height="32" src={item.icon} /></td> : <td />}
                                    <td><a href={item.audio} target="_blank">{item.name}</a></td>
                                    <td>{item.section_name}</td>
                                    <td>{item.videos}</td>
                                    <td>{item.date_created}</td>
                                    <td>
                                        <Button color="info" size="sm" tag={Link} to={`/songs/${item.id}/edit`}>Edit</Button>
                                        <Button color="danger" className="ml-1" size="sm" tag={Link} to={`/songs/${item.id}/delete`}>Delete</Button>
                                    </td>
                                </tr>
                            )) : (
                                <tr><td className="text-muted text-center" colSpan="7">No songs found.</td></tr>
                            )}
                            </tbody>
                        </Table>
                    </div>
                </div>
            )}
            <p className="text-center text-lg-left">
                Showing {data.data.length} of {data.total} songs (page {data.page} of {data.pages}).
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

Songs.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const SongsNew = ({ jwt }) => {
    const history = useHistory();
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [isSaving, setSaving] = useState(false);
    const [section, setSection] = useState(null);
    const [name, setName] = useState(null);
    const [audio, setAudio] = useState(null);
    const [icon, setIcon] = useState(null);
    const [errors, setErrors] = useState({});
    const [sections, setSections] = useState(null);
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/song-sections?count=100', { headers: { 'Authorization': `Bearer ${jwt}` } })
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get song sections data.
            </p>
        )
    } else if (sections) {
        const handleSubmit = e => {
            e.preventDefault();
            setErrors({});
            setSaving(true);
            const data = new FormData();
            data.append('name', name);
            if (section) {
                data.append('section_id', section)
            }

            if (audio) {
                data.append('audio', audio)
            }

            if (icon) {
                data.append('icon', icon)
            }

            axios.post(process.env.REACT_APP_BASE_URL + '/api/admin/songs', data, { headers: { 'Authorization': `Bearer ${jwt}` } })
                .then(() => {
                    history.push('/songs')
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
                <h1>Songs &raquo; New</h1>
                <hr />
                <Row>
                    <Col lg={10} xl={8}>
                        <Form onSubmit={handleSubmit}>
                            <FormGroup row>
                                <Label for="song-section" md={3}>Section <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <CustomInput type="select" name="section" id="song-section" invalid={errors.hasOwnProperty('section')} required onChange={e => setSection(e.target.value)}>
                                        <option value="">None</option>
                                        {sections.map(section => (
                                            <option value={section.id}>{section.name}</option>
                                        ))}
                                    </CustomInput>
                                    {errors.hasOwnProperty('section') ? <FormFeedback valid={false}>{Object.values(errors['section'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="song-name" md={3}>Name <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="name" id="song-name" invalid={errors.hasOwnProperty('name')} value={name} required onChange={e => setName(e.target.value)} />
                                    {errors.hasOwnProperty('name') ? <FormFeedback valid={false}>{Object.values(errors['name'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="song-audio" md={3}>Audio <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <CustomInput type="file" name="audio" id="song-audio" invalid={errors.hasOwnProperty('audio')} required onChange={e => setAudio(e.target.files[0])} />
                                    {errors.hasOwnProperty('audio') ? <FormFeedback valid={false}>{Object.values(errors['audio'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="song-icon" md={3}>Icon</Label>
                                <Col md={9}>
                                    <CustomInput type="file" name="icon" id="song-icon" invalid={errors.hasOwnProperty('icon')} onChange={e => setIcon(e.target.files[0])} />
                                    {errors.hasOwnProperty('icon') ? <FormFeedback valid={false}>{Object.values(errors['icon'])[0]}</FormFeedback> : null}
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

SongsNew.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const SongsEdit = ({ jwt }) => {
    const { id } = useParams();
    const history = useHistory();
    const [isErrored, setErrored] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [isSaving, setSaving] = useState(false);
    const [section, setSection] = useState(null);
    const [name, setName] = useState(null);
    const [audio, setAudio] = useState(null);
    const [icon, setIcon] = useState(null);
    const [errors, setErrors] = useState({});
    const [sections, setSections] = useState(null);
    const [song, setSong] = useState(null);
    useEffect(() => {
        setLoading(true);
        const call1 = axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/songs/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setName(data.name);
                setSection(data.section_id);
                setSong(data)
            });
        const call2 = axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/song-sections?count=100', { headers: { 'Authorization': `Bearer ${jwt}` } })
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get song sections data.
            </p>
        )
    } else if (song && sections) {
        const handleSubmit = e => {
            e.preventDefault();
            setErrors({});
            setSaving(true);
            const data = new FormData();
            data.append('_METHOD', 'PUT');
            data.append('name', name);
            if (section) {
                data.append('section_id', section)
            }

            if (audio) {
                data.append('audio', audio)
            }

            if (icon) {
                data.append('icon', icon)
            }

            axios.post(process.env.REACT_APP_BASE_URL + `/api/admin/songs/${id}`, data, { headers: { 'Authorization': `Bearer ${jwt}` } })
                .then(() => {
                    history.push('/songs')
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
                <h1>Songs &raquo; Edit</h1>
                <hr />
                <Row>
                    <Col lg={10} xl={8}>
                        <Form onSubmit={handleSubmit}>
                            <FormGroup row>
                                <Label for="song-section" md={3}>Section <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <CustomInput type="select" name="section" id="song-section" invalid={errors.hasOwnProperty('section')} required onChange={e => setSection(e.target.value)}>
                                        <option value="">None</option>
                                        {sections.map(section => (
                                            <option value={section.id} selected={section.id == song.section_id}>{section.name}</option>
                                        ))}
                                    </CustomInput>
                                    {errors.hasOwnProperty('section') ? <FormFeedback valid={false}>{Object.values(errors['section'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="song-name" md={3}>Name <span className="text-danger">*</span></Label>
                                <Col md={9}>
                                    <Input name="name" id="song-name" invalid={errors.hasOwnProperty('name')} value={name} required onChange={e => setName(e.target.value)} />
                                    {errors.hasOwnProperty('name') ? <FormFeedback valid={false}>{Object.values(errors['name'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="song-audio" md={3}>Audio</Label>
                                <Col md={9}>
                                    <CustomInput type="file" name="audio" id="song-audio" invalid={errors.hasOwnProperty('audio')} onChange={e => setAudio(e.target.files[0])} />
                                    {errors.hasOwnProperty('audio') ? <FormFeedback valid={false}>{Object.values(errors['audio'])[0]}</FormFeedback> : null}
                                </Col>
                            </FormGroup>
                            <FormGroup row>
                                <Label for="song-icon" md={3}>Icon</Label>
                                <Col md={9}>
                                    <CustomInput type="file" name="icon" id="song-icon" invalid={errors.hasOwnProperty('icon')} onChange={e => setIcon(e.target.files[0])} />
                                    {errors.hasOwnProperty('icon') ? <FormFeedback valid={false}>{Object.values(errors['icon'])[0]}</FormFeedback> : null}
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

SongsEdit.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const SongsDelete = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isDeleting, setDeleting] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [song, setSong] = useState(null);
    const handleCancel = () => history.push('/songs');
    const handleDelete = () => {
        setDeleting(true);
        axios.delete(process.env.REACT_APP_BASE_URL + `/api/admin/songs/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/songs')
            })
            .catch(() => {})
            .then(() => {
                setDeleting(false)
            })
    };
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/songs/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setSong(data);
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get song data.
            </p>
        )
    } else if (song) {
        return (
            <div>
                <h1>Songs &raquo; Delete</h1>
                <hr />
                <Alert className="p-3" color="danger">
                    <h4 className="alert-heading">Confirm</h4>
                    <p>
                        You are about to delete song <strong>{song.name}</strong> which will remove it from <strong>{song.videos} videos</strong>.
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

SongsDelete.propTypes = {
    jwt: PropTypes.string.isRequired
};
