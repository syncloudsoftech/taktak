import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Form, Input, Pagination, PaginationItem, PaginationLink, Table } from 'reactstrap';
import { Link, useHistory, useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import axios from 'axios';
import _ from 'lodash';

export const Comments = ({ jwt }) => {
    const [isLoading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [q, setQ] = useState(null);
    const [data, setData] = useState({ data: [], page, total: 0 });
    const reload = (page, q) => {
        setLoading(true);
        const params = { page, q };
        axios.get(process.env.REACT_APP_BASE_URL + '/api/admin/comments', { headers: { 'Authorization': `Bearer ${jwt}` }, params })
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
            <h1>Comments</h1>
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
                                <th>Video</th>
                                <th>User</th>
                                <th>Text</th>
                                <th>Date created</th>
                                <th />
                            </tr>
                            </thead>
                            <tbody>
                            {data.data.length > 0 ? data.data.map(item => (
                                <tr>
                                    <td>{item.id}</td>
                                    <td className="text-center">
                                        <a href={item.video_video} target="_blank">
                                            <img alt="" height="32" src={item.video_screenshot} />
                                        </a>
                                    </td>
                                    <td>
                                        <Link className="text-body" to={`/users/${item.user_id}/edit`}>
                                            @{item.user_username}
                                        </Link>
                                    </td>
                                    <td>{item.text.length > 32 ? item.text.substr(0, 32) + '…' : item.text}</td>
                                    <td>{item.date_created}</td>
                                    <td>
                                        <Button color="danger" className="ml-1" size="sm" tag={Link} to={`/comments/${item.id}/delete`}>Delete</Button>
                                    </td>
                                </tr>
                            )) : (
                                <tr><td className="text-muted text-center" colSpan="6">No comments found.</td></tr>
                            )}
                            </tbody>
                        </Table>
                    </div>
                </div>
            )}
            <p className="text-center text-lg-left">
                Showing {data.data.length} of {data.total} comments (page {data.page} of {data.pages}).
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

Comments.propTypes = {
    jwt: PropTypes.string.isRequired
};

export const CommentsDelete = ({ jwt }) => {
    const history = useHistory();
    const { id } = useParams();
    const [isErrored, setErrored] = useState(false);
    const [isDeleting, setDeleting] = useState(false);
    const [isLoading, setLoading] = useState(true);
    const [comment, setComment] = useState(null);
    const handleCancel = () => history.push('/comments');
    const handleDelete = () => {
        setDeleting(true);
        axios.delete(process.env.REACT_APP_BASE_URL + `/api/admin/comments/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(() => {
                history.push('/comments')
            })
            .catch(() => {})
            .then(() => {
                setDeleting(false)
            })
    };
    useEffect(() => {
        setLoading(true);
        axios.get(process.env.REACT_APP_BASE_URL + `/api/admin/comments/${id}`, { headers: { 'Authorization': `Bearer ${jwt}` } })
            .then(({ data }) => {
                setComment(data);
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
                <i className="fas fa-times mr-1 text-danger" /> Failed to get comment data.
            </p>
        )
    } else if (comment) {
        return (
            <div>
                <h1>Comments &raquo; Delete</h1>
                <hr />
                <Alert className="p-3" color="danger">
                    <h4 className="alert-heading">Confirm</h4>
                    <p>
                        You are about to delete comment <strong>#{comment.id}</strong> by <strong>@{comment.user_username}</strong>.
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

CommentsDelete.propTypes = {
    jwt: PropTypes.string.isRequired
};
