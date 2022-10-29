import React, { useState } from 'react';
import { Col, Collapse, Container, Nav, NavItem, Navbar, NavbarToggler, Row } from 'reactstrap';
import { HashRouter, Link, NavLink, Route, Redirect, Switch, } from 'react-router-dom';
import store from 'store';

import { Articles, ArticlesNew, ArticlesEdit, ArticlesDelete } from './articles';
import { ArticleSections, ArticleSectionsNew, ArticleSectionsEdit, ArticleSectionsDelete } from './article-sections';
import { Comments, CommentsDelete } from './comments';
import { Dashboard } from './dashboard';
import { Login } from './login';
import { NotificationsNew } from './notifications';
import { Profile } from './profile';
import { SongSections, SongSectionsNew, SongSectionsEdit, SongSectionsDelete } from './song-sections';
import { Songs, SongsNew, SongsEdit, SongsDelete } from './songs';
import { Users, UsersEdit, UsersDelete } from './users';
import { VideoSections, VideoSectionsNew, VideoSectionsEdit, VideoSectionsDelete } from './video-sections';
import { Videos, VideosEdit, VideosDelete } from './videos';

const InsecureRoute = ({ children, jwt, ...props }) => (
    <Route {...props}>
        {jwt ? <Redirect to="/" /> : children}
    </Route>
);

const SecureRoute = ({ children, jwt, ...props }) => (
    <Route {...props}>
        {jwt ? children : <Redirect to="/login" />}
    </Route>
);

export const TakTak = () => {
    const [isOpen, setIsOpen] = useState(false);
    const toggle = () => setIsOpen(!isOpen);
    const [jwt, setJwt] = useState(store.get('jwt') || '');
    const handleLogout = (e) => {
        e.preventDefault();
        handleSetJwt(null)
    };
    const handleSetJwt = (jwt) => {
        store.set('jwt', jwt);
        setJwt(jwt)
    };
    return (
        <HashRouter>
            <Navbar color="secondary" light expand="md">
                <Link className="navbar-brand" to="/">TakTak</Link>
                {jwt ? <NavbarToggler onClick={toggle} /> : null}
                {jwt ? (
                    <Collapse isOpen={isOpen} navbar>
                        <Nav className="d-md-none" navbar>
                            <NavItem>
                                <NavLink className="nav-link" exact to="/" onClick={toggle}>
                                    <i className="fas fa-chart-line fa-fw" /> Dashboard
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/users" onClick={toggle}>
                                    <i className="fas fa-users fa-fw" /> Users
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/videos" onClick={toggle}>
                                    <i className="fas fa-video fa-fw" /> Videos
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/video-sections" onClick={toggle}>
                                    <i className="fas fa-sitemap fa-fw" /> Video sections
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/songs" onClick={toggle}>
                                    <i className="fas fa-music fa-fw" /> Songs
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/song-sections" onClick={toggle}>
                                    <i className="fas fa-sitemap fa-fw" /> Song sections
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/articles" onClick={toggle}>
                                    <i className="fas fa-newspaper fa-fw" /> Articles
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/article-sections" onClick={toggle}>
                                    <i className="fas fa-sitemap fa-fw" /> Article sections
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/comments" onClick={toggle}>
                                    <i className="fas fa-comments fa-fw" /> Comments
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/notifications" onClick={toggle}>
                                    <i className="fas fa-bullhorn fa-fw" /> Notifications
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" to="/profile" onClick={toggle}>
                                    <i className="fas fa-user-circle fa-fw" /> Profile
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <a className="nav-link" href="admin/src/components/main" onClick={handleLogout}>
                                    <i className="fas fa-sign-out-alt fa-fw" /> Logout
                                </a>
                            </NavItem>
                        </Nav>
                    </Collapse>
                ) : null}
            </Navbar>
            <Container>
                <Switch>
                    <InsecureRoute jwt={jwt} path="/login">
                        <Login onLoginSuccess={handleSetJwt} />
                    </InsecureRoute>
                    <SecureRoute jwt={jwt} path="/">
                        <Row className="my-3 my-md-4 my-lg-5">
                            <Col className="d-none d-md-block" md={4} lg={3} xl={2}>
                                <Nav pills vertical>
                                    <NavItem>
                                        <NavLink className="nav-link" exact to="/">
                                            <i className="fas fa-chart-line fa-fw" /> Dashboard
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/users">
                                            <i className="fas fa-users fa-fw" /> Users
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/videos">
                                            <i className="fas fa-video fa-fw" /> Videos
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/video-sections">
                                            <i className="fas fa-sitemap fa-fw" /> Video sections
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/songs">
                                            <i className="fas fa-music fa-fw" /> Songs
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/song-sections">
                                            <i className="fas fa-sitemap fa-fw" /> Song sections
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/articles">
                                            <i className="fas fa-newspaper fa-fw" /> Articles
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/article-sections">
                                            <i className="fas fa-sitemap fa-fw" /> Article sections
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/comments">
                                            <i className="fas fa-comments fa-fw" /> Comments
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/notifications">
                                            <i className="fas fa-bullhorn fa-fw" /> Notifications
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <NavLink className="nav-link" to="/profile">
                                            <i className="fas fa-user-circle fa-fw" /> Profile
                                        </NavLink>
                                    </NavItem>
                                    <NavItem>
                                        <a className="nav-link" href="admin/src/components/main" onClick={handleLogout}>
                                            <i className="fas fa-sign-out-alt fa-fw" /> Logout
                                        </a>
                                    </NavItem>
                                </Nav>
                            </Col>
                            <Col md={8} lg={9} xl={10}>
                                <Switch>
                                    <Route path="/" exact>
                                        <Dashboard jwt={jwt} />
                                    </Route>
                                    <Route path="/users/:id/delete">
                                        <UsersDelete jwt={jwt} />
                                    </Route>
                                    <Route path="/users/:id/edit">
                                        <UsersEdit jwt={jwt} />
                                    </Route>
                                    <Route path="/users" exact>
                                        <Users jwt={jwt} />
                                    </Route>
                                    <Route path="/videos/:id/delete">
                                        <VideosDelete jwt={jwt} />
                                    </Route>
                                    <Route path="/videos/:id/edit">
                                        <VideosEdit jwt={jwt} />
                                    </Route>
                                    <Route path="/videos" exact>
                                        <Videos jwt={jwt} />
                                    </Route>
                                    <Route path="/video-sections/new">
                                        <VideoSectionsNew jwt={jwt} />
                                    </Route>
                                    <Route path="/video-sections/:id/delete">
                                        <VideoSectionsDelete jwt={jwt} />
                                    </Route>
                                    <Route path="/video-sections/:id/edit">
                                        <VideoSectionsEdit jwt={jwt} />
                                    </Route>
                                    <Route path="/video-sections" exact>
                                        <VideoSections jwt={jwt} />
                                    </Route>
                                    <Route path="/songs/new">
                                        <SongsNew jwt={jwt} />
                                    </Route>
                                    <Route path="/songs/:id/delete">
                                        <SongsDelete jwt={jwt} />
                                    </Route>
                                    <Route path="/songs/:id/edit">
                                        <SongsEdit jwt={jwt} />
                                    </Route>
                                    <Route path="/songs" exact>
                                        <Songs jwt={jwt} />
                                    </Route>
                                    <Route path="/song-sections/new">
                                        <SongSectionsNew jwt={jwt} />
                                    </Route>
                                    <Route path="/song-sections/:id/delete">
                                        <SongSectionsDelete jwt={jwt} />
                                    </Route>
                                    <Route path="/song-sections/:id/edit">
                                        <SongSectionsEdit jwt={jwt} />
                                    </Route>
                                    <Route path="/song-sections" exact>
                                        <SongSections jwt={jwt} />
                                    </Route>
                                    <Route path="/articles/new">
                                        <ArticlesNew jwt={jwt} />
                                    </Route>
                                    <Route path="/articles/:id/delete">
                                        <ArticlesDelete jwt={jwt} />
                                    </Route>
                                    <Route path="/articles/:id/edit">
                                        <ArticlesEdit jwt={jwt} />
                                    </Route>
                                    <Route path="/articles" exact>
                                        <Articles jwt={jwt} />
                                    </Route>
                                    <Route path="/article-sections/new">
                                        <ArticleSectionsNew jwt={jwt} />
                                    </Route>
                                    <Route path="/article-sections/:id/delete">
                                        <ArticleSectionsDelete jwt={jwt} />
                                    </Route>
                                    <Route path="/article-sections/:id/edit">
                                        <ArticleSectionsEdit jwt={jwt} />
                                    </Route>
                                    <Route path="/article-sections" exact>
                                        <ArticleSections jwt={jwt} />
                                    </Route>
                                    <Route path="/comments/:id/delete">
                                        <CommentsDelete jwt={jwt} />
                                    </Route>
                                    <Route path="/comments" exact>
                                        <Comments jwt={jwt} />
                                    </Route>
                                    <Route path="/notifications" exact>
                                        <NotificationsNew jwt={jwt} />
                                    </Route>
                                    <Route path="/profile">
                                        <Profile jwt={jwt} />
                                    </Route>
                                </Switch>
                            </Col>
                        </Row>
                    </SecureRoute>
                </Switch>
            </Container>
        </HashRouter>
    )
};
